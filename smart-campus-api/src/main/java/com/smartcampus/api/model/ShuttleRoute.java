package com.smartcampus.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shuttle_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShuttleRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "origin_name", nullable = false)
    private String originName;

    @Column(name = "destination_name", nullable = false)
    private String destinationName;

    @Column(name = "origin_lat", nullable = false)
    private Double originLat;

    @Column(name = "origin_lng", nullable = false)
    private Double originLng;

    @Column(name = "dest_lat", nullable = false)
    private Double destLat;

    @Column(name = "dest_lng", nullable = false)
    private Double destLng;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String polyline;

    @Column(length = 20, nullable = false)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
