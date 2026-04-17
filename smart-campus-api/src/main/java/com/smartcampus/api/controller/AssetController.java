package com.smartcampus.api.controller;

import com.smartcampus.api.dto.AssetDTO;
import com.smartcampus.api.model.Asset;
import com.smartcampus.api.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetRepository assetRepository;

    @GetMapping
    public ResponseEntity<List<AssetDTO>> getAllAssets() {
        List<AssetDTO> assets = assetRepository.findAll().stream()
                .map(a -> new AssetDTO(a.getId(), a.getName(), a.getDescription()))
                .toList();
        return ResponseEntity.ok(assets);
    }
}
