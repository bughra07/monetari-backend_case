package com.bugra.monetari.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final String expectedApiKey;

    public ApiKeyFilter(@Value("${app.security.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                      "status": 401,
                      "error": "Unauthorized - invalid or missing API key"
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/error");
    }
}