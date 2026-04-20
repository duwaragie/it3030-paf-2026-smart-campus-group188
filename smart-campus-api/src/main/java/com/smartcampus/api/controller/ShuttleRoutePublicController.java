package com.smartcampus.api.controller;

import com.smartcampus.api.dto.ShuttleRouteDTO;
import com.smartcampus.api.service.ShuttleRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shuttle/routes")
@RequiredArgsConstructor
public class ShuttleRoutePublicController {

    private final ShuttleRouteService shuttleRouteService;

    @GetMapping
    public ResponseEntity<List<ShuttleRouteDTO>> getActiveRoutes() {
        return ResponseEntity.ok(shuttleRouteService.getAllActiveRoutes());
    }
}
