package com.apexmotors.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import java.io.IOException;
import java.net.URI;

/** Uses only a validated local path, never a Host header, next parameter or absolute saved URL. */
public class LocalLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final RequestCache cache;

    public LocalLoginSuccessHandler(RequestCache cache) { this.cache = cache; }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var saved = cache.getRequest(request, response);
        cache.removeRequest(request, response);
        String context = request.getContextPath();
        String target = "/profile";
        if (saved != null) {
            try {
                String path = URI.create(saved.getRedirectUrl()).getRawPath();
                if (path != null && path.startsWith(context + "/")) {
                    path = path.substring(context.length());
                    if (path.matches("/(profile|admin|appointments|favorites)(/[A-Za-z0-9_-]+)*/?")) {
                        target = path;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Malformed saved requests fall back to the account page.
            }
        }
        response.sendRedirect(context + target);
    }
}
