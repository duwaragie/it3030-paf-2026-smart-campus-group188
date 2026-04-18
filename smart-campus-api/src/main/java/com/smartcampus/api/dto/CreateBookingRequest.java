package com.smartcampus.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    
    @NotNull(message = "Resource ID is required")
    private Long resourceId;
    
    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in the future")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "End time must be in the future")
    private LocalDateTime endTime;
    
    @NotBlank(message = "Purpose is required")
    @Size(min = 5, max = 500, message = "Purpose must be between 5 and 500 characters")
    private String purpose;
    
    @Min(value = 1, message = "Expected attendees must be at least 1")
    private Integer expectedAttendees;
}
