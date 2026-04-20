package com.smartcampus.api.dto;

import com.smartcampus.api.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSetupCompleteRequest {
    @NotBlank
    private String token;

    @NotBlank
    @StrongPassword
    private String password;
}
