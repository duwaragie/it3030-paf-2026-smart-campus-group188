package com.smartcampus.api.service;

import com.smartcampus.api.dto.ResourceDTO;
import com.smartcampus.api.exception.ResourceNotFoundException;
import com.smartcampus.api.model.Resource;
import com.smartcampus.api.repository.ResourceRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    public List<ResourceDTO> getAllResources() {
        return resourceRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public ResourceDTO getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
        return convertToDTO(resource);
    }

    public ResourceDTO createResource(ResourceDTO resourceDTO) {
        Resource resource = convertToEntity(resourceDTO);
        Resource savedResource = resourceRepository.save(resource);
        return convertToDTO(savedResource);
    }

    public ResourceDTO updateResource(Long id, ResourceDTO resourceDTO) {
        Resource existingResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        BeanUtils.copyProperties(resourceDTO, existingResource, "id");
        Resource updatedResource = resourceRepository.save(existingResource);
        return convertToDTO(updatedResource);
    }

    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }

    private ResourceDTO convertToDTO(Resource resource) {
        ResourceDTO resourceDTO = new ResourceDTO();
        BeanUtils.copyProperties(resource, resourceDTO);
        return resourceDTO;
    }

    private Resource convertToEntity(ResourceDTO resourceDTO) {
        Resource resource = new Resource();
        BeanUtils.copyProperties(resourceDTO, resource);
        return resource;
    }
}
