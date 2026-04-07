package com.smartcampus.api.dto;

import com.smartcampus.api.model.ResourceType;
import com.smartcampus.api.model.ResourceStatus;
import lombok.Data;

@Data
public class ResourceDTO {
    private Long id;
    private String name;
    private ResourceType type;
    private Integer capacity;
    private String location;
    private String availabilityWindows;
    private ResourceStatus status;
}
