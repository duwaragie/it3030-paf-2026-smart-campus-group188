package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.TicketResponseDTO;
import com.smartcampus.api.model.TicketStatus;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.TicketService;
import com.smartcampus.api.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MyTicketsTool implements AiTool {

    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_my_tickets";
    }

    @Override
    public String description() {
        return "Returns the current user's maintenance tickets. Optionally filter by status. " +
                "Use this for questions like 'how many tickets do I have', 'show my open tickets', " +
                "'any updates on my tickets'.";
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
                .add("OPEN").add("IN_PROGRESS").add("RESOLVED").add("CLOSED").add("REJECTED");
        schema.putArray("required");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        List<TicketResponseDTO> all = ticketService.getAllTickets(currentUser);
        JsonNode statusNode = arguments == null ? null : arguments.get("status");
        if (statusNode != null && !statusNode.isNull()) {
            try {
                TicketStatus filter = TicketStatus.valueOf(statusNode.asString().toUpperCase());
                all = all.stream().filter(t -> t.getStatus() == filter).toList();
            } catch (IllegalArgumentException ignored) {
                // invalid status → return unfiltered; the LLM can retry
            }
        }
        List<Map<String, Object>> slim = all.stream()
                .map(t -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getTitle());
                    m.put("status", t.getStatus());
                    m.put("priority", t.getPriority());
                    m.put("category", t.getCategory());
                    m.put("location", t.getLocation());
                    m.put("assignedToName", t.getAssignedToName());
                    m.put("createdAt", t.getCreatedAt());
                    m.put("updatedAt", t.getUpdatedAt());
                    return m;
                })
                .toList();
        return Map.of("count", slim.size(), "tickets", slim);
    }
}
