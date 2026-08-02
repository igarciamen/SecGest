package com.igarciamen.users.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiterService limiter;

    public LoginRateLimitFilter(LoginRateLimiterService limiter) {
        this.limiter = limiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        boolean isLoginAttempt = "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals("/api/auth/login");

        if (isLoginAttempt && !limiter.isAllowed(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Demasiados intentos. Espera un minuto e intentalo de nuevo.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}