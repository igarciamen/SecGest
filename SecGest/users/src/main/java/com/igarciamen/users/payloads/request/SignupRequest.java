package com.igarciamen.users.payloads.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SignupRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    public SignupRequest() {}

    public SignupRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;

    }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

}
