package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.ShuttleRouteDTO;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.ShuttleRouteService;
import com.smartcampus.api.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BrowseShuttleRoutesTool implements AiTool {

    private final ShuttleRouteService shuttleRouteService;
    private final ObjectMapper objectMapper;

    @Override public String name() { return "browse_shuttle_routes"; }

    @Override public String description() {
        return "Lists campus shuttle routes (name, origin, destination, active flag). "
                + "Defaults to only currently active routes. Use for 'what shuttle routes are there', "
                + "'is the library shuttle running', 'show all shuttle routes'. Read-only.";
    }

    @Override public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode includeInactive = props.putObject("includeInactive");
        includeInactive.put("type", "boolean");
        includeInactive.put("description", "If true, include inactive routes too. Default false.");
        schema.putArray("required");
        return schema;
    }

    @Override public Object execute(ObjectNode arguments, User currentUser) {
        boolean includeInactive = false;
        if (arguments != null) {
            JsonNode n = arguments.get("includeInactive");
            if (n != null && n.isBoolean()) includeInactive = n.asBoolean();
        }

        List<ShuttleRouteDTO> routes = includeInactive
                ? shuttleRouteService.getAllRoutes()
                : shuttleRouteService.getAllActiveRoutes();

        List<Map<String, Object>> slim = routes.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("origin", r.getOriginName());
            m.put("destination", r.getDestinationName());
            m.put("active", r.getActive());
            return m;
        }).toList();

        return Map.of("count", slim.size(), "routes", slim);
    }
}
