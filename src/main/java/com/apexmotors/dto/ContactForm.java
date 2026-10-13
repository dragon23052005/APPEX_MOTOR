package com.apexmotors.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ContactForm {

    @NotBlank(message = "Please tell us your name.")
    @Size(max = 80, message = "Name must be 80 characters or fewer.")
    private String name;

    @NotBlank(message = "Please enter your email address.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 120, message = "Email must be 120 characters or fewer.")
    private String email;

    @Pattern(regexp = "^$|^[+()0-9 .-]{7,24}$", message = "Please enter a valid phone number.")
    private String phone;

    @NotBlank(message = "Please choose an area of interest.")
    private String interest;

    @NotBlank(message = "Please include a short message.")
    @Size(min = 10, max = 1200, message = "Message must be between 10 and 1,200 characters.")
    private String message;

    private boolean privacyAccepted;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getInterest() { return interest; }
    public void setInterest(String interest) { this.interest = interest; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }
}
