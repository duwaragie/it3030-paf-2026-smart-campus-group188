package com.smartcampus.api.service;

import com.smartcampus.api.dto.ShuttleRouteDTO;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.model.ShuttleRoute;
import com.smartcampus.api.repository.ShuttleRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShuttleRouteService {

    private final ShuttleRouteRepository shuttleRouteRepository;

    @Transactional(readOnly = true)
    public List<ShuttleRouteDTO> getAllActiveRoutes() {
        return shuttleRouteRepository.findByActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShuttleRouteDTO> getAllRoutes() {
        return shuttleRouteRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ShuttleRouteDTO createRoute(ShuttleRouteDTO dto) {
        ShuttleRoute route = ShuttleRoute.builder()
                .name(dto.getName())
                .originName(dto.getOriginName())
                .destinationName(dto.getDestinationName())
                .originLat(dto.getOriginLat())
                .originLng(dto.getOriginLng())
                .destLat(dto.getDestLat())
                .destLng(dto.getDestLng())
                .polyline(dto.getPolyline())
                .color(dto.getColor())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
        
        return convertToDTO(shuttleRouteRepository.save(route));
    }

    public ShuttleRouteDTO updateRoute(Long id, ShuttleRouteDTO dto) {
        ShuttleRoute route = shuttleRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shuttle route not found with id: " + id));

        route.setName(dto.getName());
        route.setOriginName(dto.getOriginName());
        route.setDestinationName(dto.getDestinationName());
        route.setOriginLat(dto.getOriginLat());
        route.setOriginLng(dto.getOriginLng());
        route.setDestLat(dto.getDestLat());
        route.setDestLng(dto.getDestLng());
        route.setPolyline(dto.getPolyline());
        route.setColor(dto.getColor());
        route.setActive(dto.getActive());

        return convertToDTO(shuttleRouteRepository.save(route));
    }

    public ShuttleRouteDTO toggleActive(Long id) {
        ShuttleRoute route = shuttleRouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shuttle route not found with id: " + id));
        
        route.setActive(!route.getActive());
        return convertToDTO(shuttleRouteRepository.save(route));
    }

    public void deleteRoute(Long id) {
        if (!shuttleRouteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shuttle route not found with id: " + id);
        }
        shuttleRouteRepository.deleteById(id);
    }

    private ShuttleRouteDTO convertToDTO(ShuttleRoute route) {
        return ShuttleRouteDTO.builder()
                .id(route.getId())
                .name(route.getName())
                .originName(route.getOriginName())
                .destinationName(route.getDestinationName())
                .originLat(route.getOriginLat())
                .originLng(route.getOriginLng())
                .destLat(route.getDestLat())
                .destLng(route.getDestLng())
                .polyline(route.getPolyline())
                .color(route.getColor())
                .active(route.getActive())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
