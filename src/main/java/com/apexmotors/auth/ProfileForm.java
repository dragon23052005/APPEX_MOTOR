package com.apexmotors.auth;

import jakarta.validation.constraints.*;

public class ProfileForm {
    @NotBlank(message = "Enter your full name.") @Size(max = 120)
    private String fullName;
    @Size(max = 32)
    @Pattern(regexp = "[0-9+(). \\-]*", message = "Use numbers, spaces, +, brackets or hyphens.")
    private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value == null ? null : value.strip(); }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value == null ? null : value.strip(); }
}
