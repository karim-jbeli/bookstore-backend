package com.example.userservice.dto;

import com.example.userservice.model.User.UserRole;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private String phone;
    private String postalCode;
    private String city;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean active;
}