package com.example.userservice.service;


import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;



@Service
public class StorageService {

    private final Path rootLocation = Paths.get("C:/Users/KARIM/Desktop/ProjectManagement/upload");  // The directory to store uploaded files

    // Constructor: Ensure the directory exists when the service is initialized
    public StorageService() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);  // Create the directory if it doesn't exist
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    // Method to store the uploaded file
    public String store(MultipartFile file) {
        try {
            // Generate a random file name to avoid conflicts
            String fileName = Integer.toString(new Random().nextInt(1000000000));
            String ext = getFileExtension(file.getOriginalFilename()); // Get file extension
            String name = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf('.'));
            String original = name + fileName + ext;  // Generate unique file name

            // Log the file name and location for debugging
            System.out.println("****** Original File Name: " + original);
            System.out.println("****** Saving file to: " + this.rootLocation);

            // Store the file
            Files.copy(file.getInputStream(), this.rootLocation.resolve(original), StandardCopyOption.REPLACE_EXISTING);

            // Return the file name to the caller
            return original;

        } catch (IOException e) {
            // Handle specific file storage errors
            throw new RuntimeException("FAIL to store file", e);
        }
    }

    // Helper method to get the file extension safely
    private String getFileExtension(String filename) {
        int extIndex = filename.lastIndexOf('.');
        if (extIndex > 0) {
            return filename.substring(extIndex);  // Return the file extension (e.g., .jpg, .png)
        }
        return "";  // No extension found
    }

    public Resource loadFile(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("FAIL!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("FAIL!");
        }
    }
}
