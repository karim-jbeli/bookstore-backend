package com.example.userservice.jwt;


import com.example.userservice.service.UserDetailsImpl;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  @Value("${app.jwtSecret}")
  private String jwtSecret;

  @Value("${app.jwtExpirationMs}")
  private int jwtExpirationMs;

  // Getter pour la clé secrète (nécessaire pour extraire les claims)
  public String getJwtSecret() {
    return jwtSecret;
  }

  // Getter pour l'expiration
  public int getJwtExpirationMs() {
    return jwtExpirationMs;
  }

  public String generateJwtToken(UserDetailsImpl userDetails) {
    return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("userId", userDetails.getId())
            .claim("email", userDetails.getEmail())
            .claim("role", userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("USER"))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
  }

  public String generateTokenFromUsername(String username) {
    return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
  }

  public String getUserNameFromJwtToken(String token) {
    return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parser()
              .setSigningKey(jwtSecret)
              .parseClaimsJws(authToken);
      return true;
    } catch (SignatureException e) {
      logger.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }
    return false;
  }

  // Méthode utilitaire pour extraire les claims
  public Claims extractAllClaims(String token) {
    return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
  }

  // Méthode pour extraire l'ID utilisateur
  public Long extractUserId(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("userId", Long.class); // Assurez-vous d'ajouter userId dans le token
  }

  // Méthode pour extraire le rôle
  public String extractRole(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("role", String.class); // Assurez-vous d'ajouter role dans le token
  }


}
