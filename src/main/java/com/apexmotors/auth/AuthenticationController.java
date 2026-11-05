package com.apexmotors.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfException;
import java.security.Principal;
import java.util.Objects;

@Controller
@Profile("mysql")
public class AuthenticationController {
    private final AccountService accounts;
    public AuthenticationController(AccountService accounts) { this.accounts = accounts; }

    @InitBinder("registrationForm")
    void registrationBinding(WebDataBinder binder) {
        binder.setAllowedFields("fullName", "email", "password", "confirmPassword");
    }

    @GetMapping("/login")
    String login(Model model, Principal principal) {
        if (principal != null) return "redirect:/profile";
        model.addAttribute("activePage", "account");
        return "auth/login";
    }

    @GetMapping("/register")
    String register(Model model, Principal principal) {
        if (principal != null) return "redirect:/profile";
        model.addAttribute("activePage", "account");
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                    BindingResult errors, Model model, Principal principal) {
        if (principal != null) return "redirect:/profile";
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (!errors.hasErrors()) {
            try {
                accounts.register(form);
                return "redirect:/login?registered";
            } catch (AccountService.DuplicateEmailException | DataIntegrityViolationException duplicate) {
                errors.rejectValue("email", "unavailable", "Unable to register with this email. Try signing in or use another address.");
            }
        }
        model.addAttribute("activePage", "account");
        return "auth/register";
    }

    @RequestMapping("/access-denied")
    String denied(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setStatus(403);
        model.addAttribute("activePage", "account");
        boolean csrf = request.getAttribute(WebAttributes.ACCESS_DENIED_403) instanceof CsrfException;
        model.addAttribute("accountErrorTitle", csrf ? "Let’s refresh your session." : "This area is restricted.");
        model.addAttribute("accountErrorMessage", csrf
            ? "Your form session has expired or could not be verified. Open the page again and retry."
            : "Your account does not have permission to access this page.");
        return "auth/denied";
    }
}
