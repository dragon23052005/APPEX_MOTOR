package com.apexmotors.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@Controller
@Profile("mysql")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class ProfileController {
    private final AccountService accounts;
    public ProfileController(AccountService accounts) { this.accounts = accounts; }

    @InitBinder("profileForm")
    void profileBinding(WebDataBinder binder) { binder.setAllowedFields("fullName", "phone"); }

    @InitBinder("passwordForm")
    void passwordBinding(WebDataBinder binder) {
        binder.setAllowedFields("currentPassword", "newPassword", "confirmPassword");
    }

    @GetMapping("/profile")
    String profile(Authentication authentication, Model model) {
        return profilePage(authentication, model);
    }

    @PostMapping("/profile")
    String update(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult errors,
                  Authentication authentication, Model model) {
        if (errors.hasErrors()) return profilePage(authentication, model);
        accounts.updateProfile(authentication.getName(), form);
        return "redirect:/profile?updated";
    }

    @PostMapping("/profile/password")
    String password(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult errors,
                    Authentication authentication, Model model,
                    HttpServletRequest request, HttpServletResponse response) {
        if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (Objects.equals(form.getCurrentPassword(), form.getNewPassword())) {
            errors.rejectValue("newPassword", "unchanged", "Choose a password different from your current password.");
        }
        if (!errors.hasErrors()) {
            if (accounts.changePassword(authentication.getName(), form)) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                return "redirect:/login?passwordChanged";
            }
            errors.rejectValue("currentPassword", "incorrect", "Your current password is incorrect.");
        }
        return profilePage(authentication, model);
    }

    private String profilePage(Authentication authentication, Model model) {
        var account = accounts.profile(authentication.getName());
        model.addAttribute("account", account);
        model.addAttribute("activePage", "account");
        if (!model.containsAttribute("profileForm")) {
            var form = new ProfileForm();
            form.setFullName(account.fullName());
            form.setPhone(account.phone());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("passwordForm")) model.addAttribute("passwordForm", new PasswordChangeForm());
        return "auth/profile";
    }
}
