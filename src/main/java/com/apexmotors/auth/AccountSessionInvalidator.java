package com.apexmotors.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("mysql")
public class AccountSessionInvalidator {
    private final SessionRegistry sessions;

    public AccountSessionInvalidator(SessionRegistry sessions) { this.sessions = sessions; }

    @TransactionalEventListener
    public void afterPasswordChange(AccountService.PasswordChanged event) {
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof UserDetails user && user.getUsername().equals(event.email())) {
                sessions.getAllSessions(principal, false).forEach(session -> session.expireNow());
            }
        }
    }
}
