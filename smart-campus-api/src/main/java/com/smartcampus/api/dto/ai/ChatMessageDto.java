package com.smartcampus.api.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageDto(
        @NotBlank String role,
        String content
) {}
