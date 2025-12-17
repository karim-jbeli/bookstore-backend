package com.example.bookservice.dto;

import lombok.Data;

@Data
public class EditorDTO {
    private Long id;
    private String name;
    private String address;
    private String website;
    private String email;
    private String phone;
}