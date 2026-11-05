package com.apexmotors.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@Profile("!mysql")
public class AccountsUnavailableController {
    @RequestMapping({"/login", "/register", "/logout", "/profile", "/profile/**",
        "/admin", "/admin/**", "/appointments", "/appointments/**", "/favorites", "/favorites/**"})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    String unavailable(Model model) {
        model.addAttribute("activePage", "account");
        return "auth/unavailable";
    }
}
