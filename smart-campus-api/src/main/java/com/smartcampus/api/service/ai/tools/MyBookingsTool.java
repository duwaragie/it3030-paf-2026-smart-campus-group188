package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.BookingDTO;
import com.smartcampus.api.model.BookingStatus;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.BookingService;
import com.smartcampus.api.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MyBookingsTool implements AiTool {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_my_bookings";
    }

    @Override
    public String description() {
        return "Returns the current user's facility bookings (newest first). Optionally filter by status. " +
                "Use for 'show my bookings', 'any pending bookings', 'did my Lab 304 booking get approved'.";
    }

    @Override
    public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode status = props.putObject("status");
        status.put("type", "string");
        status.put("description", "Optional status filter");
        status.putArray("enum")
                .add("PENDING").add("APPROVED").add("REJECTED").add("CANCELLED");
        schema.putArray("required");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        BookingStatus filter = null;
        if (arguments != null) {
            JsonNode statusNode = arguments.get("status");
            if (statusNode != null && !statusNode.isNull()) {
                try {
                    filter = BookingStatus.valueOf(statusNode.asString().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // invalid status → no filter
                }
            }
        }
        List<BookingDTO> list = bookingService
                .getUserBookings(currentUser.getId(), filter, PageRequest.of(0, 50))
                .getContent();
        List<Map<String, Object>> slim = list.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("resourceName", b.getResourceName());
            m.put("locationName", b.getLocationName());
            m.put("startTime", b.getStartTime());
            m.put("endTime", b.getEndTime());
            m.put("purpose", b.getPurpose());
            m.put("status", b.getStatus());
            m.put("rejectionReason", b.getRejectionReason());
            m.put("approvedByName", b.getApprovedByName());
            return m;
        }).toList();
        return Map.of("count", slim.size(), "bookings", slim);
    }
}
