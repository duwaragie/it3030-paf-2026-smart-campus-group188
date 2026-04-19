package com.smartcampus.api.service.ai;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, AiTool> toolsByName;

    public ToolRegistry(List<AiTool> tools) {
        Map<String, AiTool> map = new HashMap<>();
        for (AiTool t : tools) {
            map.put(t.name(), t);
        }
        this.toolsByName = Map.copyOf(map);
    }

    public List<AiTool> all() {
        return List.copyOf(toolsByName.values());
    }

    public AiTool get(String name) {
        return toolsByName.get(name);
    }
}
