package com.smartcampus.api.controller;

import com.smartcampus.api.dto.AccountSetupCompleteRequest;
import com.smartcampus.api.dto.AccountSetupValidationResponse;
import com.smartcampus.api.dto.ErrorResponse;
import com.smartcampus.api.service.AccountSetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account-setup")
@RequiredArgsConstructor
public class AccountSetupController {

    private final AccountSetupService accountSetupService;

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String token) {
        try {
            AccountSetupValidationResponse response = accountSetupService.validate(token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> complete(@Valid @RequestBody AccountSetupCompleteRequest request) {
        try {
            accountSetupService.complete(request);
            return ResponseEntity.ok(Map.of("message", "Account setup complete. You can now log in."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }
}
