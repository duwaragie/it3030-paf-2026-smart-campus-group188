package com.smartcampus.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveBookingRequest {
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
}
