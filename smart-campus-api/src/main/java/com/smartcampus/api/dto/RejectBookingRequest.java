package com.smartcampus.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectBookingRequest {
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
    
    @NotBlank(message = "Rejection reason is required")
    @Size(min = 5, max = 500, message = "Rejection reason must be between 5 and 500 characters")
    private String rejectionReason;
}
