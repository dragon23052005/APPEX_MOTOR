package com.apexmotors.auth;

import java.security.Principal;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AccountNavigationAdvice {
    private final Environment environment;
    public AccountNavigationAdvice(Environment environment) { this.environment = environment; }

    @ModelAttribute
    void navigation(Model model, Principal principal) {
        model.addAttribute("accountsEnabled", environment.acceptsProfiles(Profiles.of("mysql")));
        model.addAttribute("accountSignedIn", principal != null);
    }
}
