package com.smartcampus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShuttleRouteDTO {
    private Long id;

    @NotBlank(message = "Route name is required")
    private String name;

    @NotBlank(message = "Origin name is required")
    private String originName;

    @NotBlank(message = "Destination name is required")
    private String destinationName;

    @NotNull(message = "Origin latitude is required")
    private Double originLat;

    @NotNull(message = "Origin longitude is required")
    private Double originLng;

    @NotNull(message = "Destination latitude is required")
    private Double destLat;

    @NotNull(message = "Destination longitude is required")
    private Double destLng;

    @NotBlank(message = "Polyline string is required")
    private String polyline;

    @NotBlank(message = "Color is required")
    private String color;

    @NotNull(message = "Active status is required")
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
