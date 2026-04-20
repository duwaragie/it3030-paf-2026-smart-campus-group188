package com.smartcampus.api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String code = "oauth_failed";
        String message = "Sign-in with Google failed. Please try again.";

        if (exception instanceof OAuth2AuthenticationException oae && oae.getError() != null) {
            if (oae.getError().getErrorCode() != null) {
                code = oae.getError().getErrorCode();
            }
            if (oae.getError().getDescription() != null) {
                message = oae.getError().getDescription();
            }
        }

        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String targetUrl = UriComponentsBuilder.fromUriString(base + "/login")
                .queryParam("error", java.net.URLEncoder.encode(code, StandardCharsets.UTF_8))
                .queryParam("message", java.net.URLEncoder.encode(message, StandardCharsets.UTF_8))
                .build(true)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
