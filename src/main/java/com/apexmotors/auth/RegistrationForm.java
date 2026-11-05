package com.apexmotors.auth;

import jakarta.validation.constraints.*;

public class RegistrationForm {
    @NotBlank(message = "Enter your full name.") @Size(max = 120)
    private String fullName;
    @NotBlank(message = "Enter your email address.") @Email(message = "Enter a valid email address.") @Size(max = 254)
    private String email;
    @StrongPassword
    private String password;
    @NotBlank(message = "Confirm your password.") @Size(max = 64)
    private String confirmPassword;

    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value == null ? null : value.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = AccountService.normalizeEmail(value); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
}
