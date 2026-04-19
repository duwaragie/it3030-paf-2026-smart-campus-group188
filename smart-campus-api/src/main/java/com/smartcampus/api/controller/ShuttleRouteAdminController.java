package com.smartcampus.api.controller;

import com.smartcampus.api.dto.ShuttleRouteDTO;
import com.smartcampus.api.service.ShuttleRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shuttle/routes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ShuttleRouteAdminController {

    private final ShuttleRouteService shuttleRouteService;

    @GetMapping
    public ResponseEntity<List<ShuttleRouteDTO>> getAllRoutes() {
        return ResponseEntity.ok(shuttleRouteService.getAllRoutes());
    }

    @PostMapping
    public ResponseEntity<ShuttleRouteDTO> createRoute(@Valid @RequestBody ShuttleRouteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shuttleRouteService.createRoute(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShuttleRouteDTO> updateRoute(@PathVariable Long id, @Valid @RequestBody ShuttleRouteDTO dto) {
        return ResponseEntity.ok(shuttleRouteService.updateRoute(id, dto));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ShuttleRouteDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(shuttleRouteService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        shuttleRouteService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
