package com.apexmotors.auth;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
@Profile("mysql")
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12) {
            @Override
            public boolean matches(CharSequence raw, String encoded) {
                // BCrypt must never truncate or throw a 500 for oversized login input.
                return raw != null && raw.toString().getBytes(StandardCharsets.UTF_8).length <= 72
                    && super.matches(raw, encoded);
            }
        };
    }

    @Bean
    SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    SecurityFilterChain accountSecurity(HttpSecurity http, DatabaseUserDetailsService users,
                                        PasswordEncoder passwords, SessionRegistry sessions) throws Exception {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        var cache = new HttpSessionRequestCache();
        cache.setRequestMatcher(request -> "GET".equals(request.getMethod())
            && request.getRequestURI().substring(request.getContextPath().length())
                .matches("/(profile|admin|appointments|favorites)(/.*)?"));
        http.authenticationProvider(provider)
            .authorizeHttpRequests(rules -> rules
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/profile", "/profile/**", "/appointments", "/appointments/**",
                    "/favorites", "/favorites/**").hasAnyRole("CUSTOMER", "ADMIN")
                // All other routes remain the existing public showroom, static assets or 404s.
                .anyRequest().permitAll())
            .csrf(Customizer.withDefaults())
            .requestCache(requestCache -> requestCache.requestCache(cache))
            .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/login")
                .usernameParameter("email")
                .successHandler(new LocalLoginSuccessHandler(cache))
                .failureHandler((request, response, exception) ->
                    response.sendRedirect(request.getContextPath() + "/login?error"))
                .permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
            .exceptionHandling(errors -> errors.accessDeniedPage("/access-denied")
                .authenticationEntryPoint((request, response, exception) ->
                    response.sendRedirect(request.getContextPath() + "/login")))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId())
                .maximumSessions(-1).sessionRegistry(sessions).expiredUrl("/login?expired"));
        return http.build();
    }
}
