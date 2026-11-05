package com.apexmotors.auth;

import com.apexmotors.persistence.entity.RoleName;
import com.apexmotors.persistence.entity.UserEntity;
import com.apexmotors.persistence.repository.RoleJpaRepository;
import com.apexmotors.persistence.repository.UserJpaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@Profile("mysql")
@Transactional(readOnly = true)
public class AccountService {
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;
    private final PasswordEncoder passwords;
    private final ApplicationEventPublisher events;

    public AccountService(UserJpaRepository users, RoleJpaRepository roles,
                          PasswordEncoder passwords, ApplicationEventPublisher events) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.events = events;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void register(RegistrationForm form) {
        String email = normalizeEmail(form.getEmail());
        if (users.existsByEmail(email)) throw new DuplicateEmailException();
        var user = new UserEntity();
        user.setFullName(form.getFullName());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(form.getPassword()));
        user.setRole(roles.findByName(RoleName.CUSTOMER)
            .orElseThrow(() -> new IllegalStateException("Customer role migration is missing")));
        user.setEnabled(true);
        // The unique database constraint is also enforced for concurrent registrations.
        users.saveAndFlush(user);
    }

    public AccountView profile(String authenticatedEmail) {
        var user = enabledUser(authenticatedEmail, false);
        return new AccountView(user.getFullName(), user.getEmail(), user.getPhone(), user.getRole().getName().name());
    }

    @Transactional
    public void updateProfile(String authenticatedEmail, ProfileForm form) {
        var user = enabledUser(authenticatedEmail, true);
        user.setFullName(form.getFullName());
        user.setPhone(form.getPhone());
    }

    @Transactional
    public boolean changePassword(String authenticatedEmail, PasswordChangeForm form) {
        var user = enabledUser(authenticatedEmail, true);
        if (!passwords.matches(form.getCurrentPassword(), user.getPasswordHash())) return false;
        user.setPasswordHash(passwords.encode(form.getNewPassword()));
        events.publishEvent(new PasswordChanged(user.getEmail()));
        return true;
    }

    private UserEntity enabledUser(String email, boolean lock) {
        var user = (lock ? users.lockByEmail(email) : users.findByEmail(email))
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
        if (!user.isEnabled()) throw new AccessDeniedException("Account unavailable");
        return user;
    }

    public record AccountView(String fullName, String email, String phone, String role) {}
    public record PasswordChanged(String email) {}
    public static class DuplicateEmailException extends RuntimeException {}
}
