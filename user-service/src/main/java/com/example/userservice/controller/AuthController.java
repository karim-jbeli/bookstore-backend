package com.example.userservice.controller;

import com.example.userservice.Payload.Response.JwtResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.jwt.JwtUtils;
import com.example.userservice.model.RefreshToken;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.RefreshTokenService;
import com.example.userservice.service.UserDetailsImpl;
import com.example.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> signup(@RequestBody RegisterRequest request) {
        try {
            UserResponse response = userService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify JWT token validity")
    public ResponseEntity<Map<String, Object>> verifyToken(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("message", "No token provided");
            return ResponseEntity.status(401).body(response);
        }

        String token = authHeader.substring(7);

        try {
            // 1. Valider le token avec JwtUtils
            if (!jwtUtils.validateJwtToken(token)) {
                response.put("valid", false);
                response.put("message", "Invalid or expired token");
                return ResponseEntity.status(401).body(response);
            }

            // 2. Extraire le username du token
            String username = jwtUtils.getUserNameFromJwtToken(token);

            // 3. Vérifier que l'utilisateur existe
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                response.put("valid", false);
                response.put("message", "User not found");
                return ResponseEntity.status(401).body(response);
            }

            // 4. Extraire la date d'émission du token (issuedAt)
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtUtils.getJwtSecret()) // Vous aurez besoin d'un getter
                    .parseClaimsJws(token)
                    .getBody();

            Date issuedAt = claims.getIssuedAt();

            // 5. Vérifier si l'utilisateur s'est déconnecté APRÈS création du token
            if (user.getLastLogout() != null && issuedAt != null) {
                if (issuedAt.before(java.sql.Timestamp.valueOf(user.getLastLogout()))) {
                    response.put("valid", false);
                    response.put("message", "Token invalidated due to logout");
                    return ResponseEntity.status(401).body(response);
                }
            }

            // 6. Récupérer l'expiration
            Date expiration = claims.getExpiration();

            // ✅ Token valide
            response.put("valid", true);
            response.put("username", username);
            response.put("userId", user.getId());
            response.put("role", user.getRole());
            response.put("email", user.getEmail());
            response.put("issuedAt", issuedAt);
            response.put("expiresAt", expiration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "Invalid token: " + e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }
    //****************************************************

    @PostMapping("/signin")
    @Operation(summary = "Authenticate user with JWT and refresh token")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Mettre à jour lastLogin
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Authentifier avec AuthenticationManager pour Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Générer JWT
        String jwt = jwtUtils.generateJwtToken(userDetails);

        // Créer refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        // Récupérer rôle principal
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                refreshToken.getToken(),
                "Bearer",
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles.get(0)
        ));
    }


    //****************************************************

    @GetMapping("/signout")
    @Operation(summary = "Logout user (invalidate token and refresh tokens)")
    public ResponseEntity<Map<String, String>> logoutUser(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // 1. Valider le token avec JwtUtils
                if (!jwtUtils.validateJwtToken(token)) {
                    response.put("message", "Invalid token, but logout recorded");
                    // On continue quand même pour enregistrer la déconnexion
                }

                // 2. Extraire le username
                String username = jwtUtils.getUserNameFromJwtToken(token);

                // 3. Récupérer l'utilisateur
                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null) {
                    // 4. Mettre à jour lastLogout
                    user.setLastLogout(LocalDateTime.now());
                    userRepository.save(user);

                    // 5. Supprimer les refresh tokens
                    refreshTokenService.deleteByUserId(user.getId());

                    // Log
                    System.out.println("User " + username + " logged out at " + LocalDateTime.now());

                    response.put("message", "Successfully logged out");
                    response.put("logoutTime", LocalDateTime.now().toString());
                } else {
                    response.put("message", "User not found, but logout attempted");
                }

                response.put("instruction", "Please remove the token from client storage");

            } catch (Exception e) {
                // Même en cas d'erreur, on renvoie un message
                System.out.println("Error during logout: " + e.getMessage());
                response.put("message", "Logout attempted with invalid token");
            }
        } else {
            response.put("message", "No token provided");
        }

        return ResponseEntity.ok(response);
    }
}