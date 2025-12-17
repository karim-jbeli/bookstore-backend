package com.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100)
    @Email
    private String email;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 200)
    private String address;

    @Size(max = 20)
    private String phone;

    @Size(max = 10)
    private String postalCode;

    @Size(max = 100)
    private String city;
}