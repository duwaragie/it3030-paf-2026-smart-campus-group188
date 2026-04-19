package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.NotificationDTO;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.NotificationService;
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
public class MyNotificationsTool implements AiTool {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_my_notifications";
    }

    @Override
    public String description() {
        return "Returns the current user's in-app notifications (newest first) and unread count. " +
                "Optional unreadOnly filter. Use for 'what are my notifications', 'any new updates', " +
                "'how many unread notifications'.";
    }

    @Override
    public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode unreadOnly = props.putObject("unreadOnly");
        unreadOnly.put("type", "boolean");
        unreadOnly.put("description", "If true, return only unread notifications.");
        ObjectNode limit = props.putObject("limit");
        limit.put("type", "integer");
        limit.put("description", "Max notifications to return (1\u2013100, default 20).");
        schema.putArray("required");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        Long userId = currentUser.getId();
        int limit = 20;
        boolean unreadOnly = false;
        if (arguments != null) {
            JsonNode limitNode = arguments.get("limit");
            if (limitNode != null && limitNode.isIntegralNumber()) {
                limit = Math.max(1, Math.min(100, limitNode.asInt()));
            }
            JsonNode unreadNode = arguments.get("unreadOnly");
            if (unreadNode != null && unreadNode.isBoolean()) {
                unreadOnly = unreadNode.asBoolean();
            }
        }
        List<NotificationDTO> list = notificationService.listForUser(userId, limit);
        if (unreadOnly) {
            list = list.stream().filter(n -> !Boolean.TRUE.equals(n.getRead())).toList();
        }
        long unreadCount = notificationService.unreadCount(userId);
        List<Map<String, Object>> slim = list.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("priority", n.getPriority());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("read", n.getRead());
            m.put("createdAt", n.getCreatedAt());
            return m;
        }).toList();
        return Map.of(
                "unreadCount", unreadCount,
                "returned", slim.size(),
                "notifications", slim
        );
    }
}
