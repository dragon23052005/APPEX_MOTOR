package com.apexmotors.auth;

import jakarta.validation.constraints.*;

public class PasswordChangeForm {
    @NotBlank(message = "Enter your current password.") @Size(max = 72)
    private String currentPassword;
    @StrongPassword
    private String newPassword;
    @NotBlank(message = "Confirm your new password.") @Size(max = 64)
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String value) { currentPassword = value; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String value) { newPassword = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
}
