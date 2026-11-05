package com.apexmotors.auth;

import com.apexmotors.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserJpaRepository users;

    public DatabaseUserDetailsService(UserJpaRepository users) { this.users = users; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String suppliedEmail) throws UsernameNotFoundException {
        String email = AccountService.normalizeEmail(suppliedEmail);
        if (email == null || email.length() > 254) throw new UsernameNotFoundException("Invalid credentials");
        var user = users.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(user.getEmail()).password(user.getPasswordHash())
            .roles(user.getRole().getName().name()).disabled(!user.isEnabled()).build();
    }
}
