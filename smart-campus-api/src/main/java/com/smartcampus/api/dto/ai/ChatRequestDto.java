package com.smartcampus.api.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRequestDto(
        @NotEmpty @Valid List<ChatMessageDto> messages
) {}
