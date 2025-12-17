package com.example.userservice.Payload.Response;

public class MessageResponse {
    private String message;

    //getter&setter

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    //constructor

    public MessageResponse(String message) {
        this.message = message;
    }

    public MessageResponse() {
    }
}
