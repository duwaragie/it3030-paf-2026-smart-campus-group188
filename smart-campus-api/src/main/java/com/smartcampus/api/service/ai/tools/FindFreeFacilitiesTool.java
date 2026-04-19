package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.ResourceDTO;
import com.smartcampus.api.model.ResourceStatus;
import com.smartcampus.api.model.ResourceType;
import com.smartcampus.api.model.User;
import com.smartcampus.api.repository.BookingRepository;
import com.smartcampus.api.service.ResourceService;
import com.smartcampus.api.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FindFreeFacilitiesTool implements AiTool {

    private final ResourceService resourceService;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "find_free_facilities";
    }

    @Override
    public String description() {
        return "Finds facilities (lecture halls, labs, meeting rooms, equipment) that are free in a given "
                + "time window \u2014 i.e. have no APPROVED bookings overlapping. Use for 'what labs are free "
                + "tomorrow at 2pm', 'any meeting rooms free Friday 10\u201311am', 'find a lab for 30 people Monday'. "
                + "Required: date. Optional: startTime, endTime (HH:mm), resourceType, minCapacity.";
    }

    @Override
    public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ObjectNode date = props.putObject("date");
        date.put("type", "string");
        date.put("description", "ISO date yyyy-MM-dd, e.g. 2026-04-21");

        ObjectNode startTime = props.putObject("startTime");
        startTime.put("type", "string");
        startTime.put("description", "Start time HH:mm (24h). If omitted, assumes 08:00.");

        ObjectNode endTime = props.putObject("endTime");
        endTime.put("type", "string");
        endTime.put("description", "End time HH:mm (24h). If omitted, assumes 18:00.");

        ObjectNode resourceType = props.putObject("resourceType");
        resourceType.put("type", "string");
        resourceType.putArray("enum")
                .add("LECTURE_HALL").add("LAB").add("MEETING_ROOM").add("EQUIPMENT");

        ObjectNode minCapacity = props.putObject("minCapacity");
        minCapacity.put("type", "integer");
        minCapacity.put("description", "Minimum capacity required.");

        schema.putArray("required").add("date");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        if (arguments == null || arguments.get("date") == null || arguments.get("date").isNull()) {
            return Map.of("error", "missing_date", "message", "Please provide a date (yyyy-MM-dd).");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(arguments.get("date").asString());
        } catch (RuntimeException e) {
            return Map.of("error", "invalid_date",
                    "message", "Date must be in yyyy-MM-dd format, e.g. 2026-04-21.");
        }

        LocalTime startT = parseTime(arguments.get("startTime"), LocalTime.of(8, 0));
        LocalTime endT = parseTime(arguments.get("endTime"), LocalTime.of(18, 0));
        if (!endT.isAfter(startT)) {
            return Map.of("error", "invalid_window",
                    "message", "End time must be after start time.");
        }

        ResourceType type = null;
        JsonNode typeNode = arguments.get("resourceType");
        if (typeNode != null && !typeNode.isNull() && !typeNode.asString().isBlank()) {
            try {
                type = ResourceType.valueOf(typeNode.asString().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // invalid type → no filter
            }
        }

        Integer minCapacity = null;
        JsonNode capNode = arguments.get("minCapacity");
        if (capNode != null && capNode.isIntegralNumber()) {
            minCapacity = capNode.asInt();
        }

        LocalDateTime windowStart = LocalDateTime.of(date, startT);
        LocalDateTime windowEnd = LocalDateTime.of(date, endT);

        List<ResourceDTO> candidates = resourceService.searchResources(
                type, ResourceStatus.ACTIVE, null, minCapacity, null, null);

        List<Map<String, Object>> free = candidates.stream()
                .filter(r -> bookingRepository
                        .findConflictingBookings(r.getId(), windowStart, windowEnd)
                        .isEmpty())
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("resourceId", r.getId());
                    m.put("name", r.getName());
                    m.put("type", r.getType());
                    m.put("capacity", r.getCapacity());
                    m.put("location", r.getLocationName());
                    return m;
                })
                .toList();

        Map<String, Object> window = new LinkedHashMap<>();
        window.put("date", date.toString());
        window.put("startTime", startT.toString());
        window.put("endTime", endT.toString());

        return Map.of(
                "window", window,
                "filters", Map.of(
                        "resourceType", type == null ? "ANY" : type,
                        "minCapacity", minCapacity == null ? 0 : minCapacity),
                "count", free.size(),
                "freeFacilities", free);
    }

    private LocalTime parseTime(JsonNode node, LocalTime fallback) {
        if (node == null || node.isNull() || node.asString().isBlank()) return fallback;
        try {
            return LocalTime.parse(node.asString());
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
