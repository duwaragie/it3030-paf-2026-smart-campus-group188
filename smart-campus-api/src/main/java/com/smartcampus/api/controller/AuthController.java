package com.smartcampus.api.controller;
import com.smartcampus.api.model.User;
import com.smartcampus.api.dto.UserDTO;
import com.smartcampus.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Please use /oauth2/authorization/google to login");
        response.put("loginUrl", "/oauth2/authorization/google");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "running");
        response.put("phase", "oauth2");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        // Principal is the User object set in JwtAuthFilter
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

        @PostMapping("/logout")
        public ResponseEntity<Map<String, String>> logout() {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        }
}
