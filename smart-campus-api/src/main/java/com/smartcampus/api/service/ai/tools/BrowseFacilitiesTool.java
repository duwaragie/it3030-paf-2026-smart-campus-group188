package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.ResourceDTO;
import com.smartcampus.api.model.ResourceStatus;
import com.smartcampus.api.model.ResourceType;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.ResourceService;
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
public class BrowseFacilitiesTool implements AiTool {

    private final ResourceService resourceService;
    private final ObjectMapper objectMapper;

    @Override public String name() { return "browse_facilities"; }

    @Override public String description() {
        return "Lists campus facilities (lecture halls, labs, meeting rooms, equipment). Defaults to ACTIVE "
                + "facilities only \u2014 pass status=ALL or a specific status to include maintenance/out-of-service. "
                + "Use for 'how many labs are there', 'where is Lab 304'. Read-only.";
    }

    @Override public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode t = props.putObject("resourceType");
        t.put("type", "string");
        t.putArray("enum").add("LECTURE_HALL").add("LAB").add("MEETING_ROOM").add("EQUIPMENT");
        ObjectNode st = props.putObject("status");
        st.put("type", "string");
        st.put("description", "Defaults to ACTIVE. Pass 'ALL' to see every status.");
        st.putArray("enum").add("ACTIVE").add("UNDER_MAINTENANCE").add("OUT_OF_SERVICE").add("ALL");
        ObjectNode cap = props.putObject("minCapacity");
        cap.put("type", "integer");
        ObjectNode search = props.putObject("search");
        search.put("type", "string");
        search.put("description", "Case-insensitive substring match against facility name.");
        schema.putArray("required");
        return schema;
    }

    @Override public Object execute(ObjectNode arguments, User currentUser) {
        ResourceType type = enumFrom(arguments, "resourceType", ResourceType.class);
        // Default to ACTIVE-only unless caller explicitly asks for "ALL" or a specific status.
        ResourceStatus status = ResourceStatus.ACTIVE;
        if (arguments != null) {
            JsonNode s = arguments.get("status");
            if (s != null && !s.isNull() && !s.asString().isBlank()) {
                String raw = s.asString().trim().toUpperCase();
                if ("ALL".equals(raw)) {
                    status = null;
                } else {
                    try { status = ResourceStatus.valueOf(raw); }
                    catch (IllegalArgumentException ignored) { /* keep ACTIVE default */ }
                }
            }
        }
        Integer minCapacity = null;
        if (arguments != null) {
            JsonNode cap = arguments.get("minCapacity");
            if (cap != null && cap.isIntegralNumber()) minCapacity = cap.asInt();
        }

        List<ResourceDTO> list = resourceService.searchResources(type, status, null, minCapacity, null, null);

        String search = null;
        if (arguments != null) {
            JsonNode s = arguments.get("search");
            if (s != null && !s.isNull() && !s.asString().isBlank()) search = s.asString().toLowerCase();
        }
        if (search != null) {
            final String needle = search;
            list = list.stream().filter(r -> r.getName() != null
                    && r.getName().toLowerCase().contains(needle)).toList();
        }

        Map<String, Long> countsByType = new LinkedHashMap<>();
        for (ResourceType rt : ResourceType.values()) {
            long c = list.stream().filter(r -> r.getType() == rt).count();
            if (c > 0) countsByType.put(rt.name(), c);
        }

        List<Map<String, Object>> slim = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("type", r.getType());
            m.put("capacity", r.getCapacity());
            m.put("location", r.getLocationName());
            m.put("status", r.getStatus());
            return m;
        }).toList();

        return Map.of(
                "count", slim.size(),
                "countsByType", countsByType,
                "facilities", slim);
    }

    private <E extends Enum<E>> E enumFrom(ObjectNode args, String key, Class<E> type) {
        if (args == null) return null;
        JsonNode n = args.get(key);
        if (n == null || n.isNull() || n.asString().isBlank()) return null;
        try { return Enum.valueOf(type, n.asString().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
