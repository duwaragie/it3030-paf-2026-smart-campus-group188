package com.smartcampus.api.controller;

import com.smartcampus.api.dto.*;
import com.smartcampus.api.exception.TokenRefreshException;
import com.smartcampus.api.model.RefreshToken;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.AuthService;
import com.smartcampus.api.service.OtpService;
import com.smartcampus.api.service.PasswordResetService;
import com.smartcampus.api.service.UserService;
import jakarta.validation.Valid;
import com.smartcampus.api.security.JwtService;
import com.smartcampus.api.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthService authService;
    private final OtpService otpService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            String msg = e.getMessage();

            if (msg != null && msg.contains("No account found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(msg, HttpStatus.NOT_FOUND.value()));
            }
            if (msg != null && msg.contains("not verified")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(msg, HttpStatus.FORBIDDEN.value()));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid password.", HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Registration successful. Please check your email for the OTP."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            authService.verifyEmail(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now login."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail());
            otpService.generateAndSendOtp(user, null);
            return ResponseEntity.ok(Map.of("message", "A new verification code has been sent to your email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("wait")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
            }
        }
        return ResponseEntity.ok(Map.of("message", "If an account exists with that email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", "running", "phase", "oauth2"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(oldToken -> {
                    RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldToken);
                    String accessToken = jwtService.generateToken(newToken.getUser());
                    return ResponseEntity.ok(new TokenRefreshResponse(accessToken, newToken.getToken()));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        refreshTokenService.deleteByUserId(user.getId());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
