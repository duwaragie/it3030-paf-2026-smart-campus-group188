package com.smartcampus.api.controller;

import com.smartcampus.api.dto.AmenityDTO;
import com.smartcampus.api.model.Amenity;
import com.smartcampus.api.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityRepository amenityRepository;

    @GetMapping
    public ResponseEntity<List<AmenityDTO>> getAllAmenities() {
        List<AmenityDTO> amenities = amenityRepository.findAll().stream()
                .map(a -> new AmenityDTO(a.getId(), a.getName(), a.getDescription()))
                .toList();
        return ResponseEntity.ok(amenities);
    }
}
