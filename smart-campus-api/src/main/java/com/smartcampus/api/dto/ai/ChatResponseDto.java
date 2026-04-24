package com.smartcampus.api.dto.ai;

import java.util.List;

public record ChatResponseDto(
        String reply,
        List<String> toolsUsed
) {}
