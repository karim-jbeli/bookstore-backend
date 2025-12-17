package com.example.bookservice.dto;


import lombok.Data;
import java.time.LocalDate;

@Data
public class AuthorDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String biography;
    private LocalDate birthDate;
    private String nationality;
    private String photoUrl;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}