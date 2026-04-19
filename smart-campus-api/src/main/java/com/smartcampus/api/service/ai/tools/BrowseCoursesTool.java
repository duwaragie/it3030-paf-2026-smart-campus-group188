package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.CourseOfferingDTO;
import com.smartcampus.api.model.CourseOfferingStatus;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.CourseOfferingService;
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
public class BrowseCoursesTool implements AiTool {

    private final CourseOfferingService courseOfferingService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "browse_course_offerings";
    }

    @Override
    public String description() {
        return "Browses the course catalogue. Returns course offerings (module code, title, "
                + "credits, prerequisites, lecturers, capacity). Defaults to currently OPEN offerings. "
                + "Use for 'what courses can I enroll in', 'is CS2030 offered this semester', "
                + "'who teaches Database Systems'. Optional filters: semester, status, search term.";
    }

    @Override
    public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ObjectNode semester = props.putObject("semester");
        semester.put("type", "string");
        semester.put("description", "Optional semester filter, e.g. '2026-S1'.");

        ObjectNode status = props.putObject("status");
        status.put("type", "string");
        status.put("description", "Optional status filter. Defaults to OPEN if not provided.");
        status.putArray("enum").add("DRAFT").add("OPEN").add("CLOSED").add("ARCHIVED");

        ObjectNode search = props.putObject("search");
        search.put("type", "string");
        search.put("description", "Optional case-insensitive substring match against course code or title.");

        schema.putArray("required");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        String semester = null;
        CourseOfferingStatus status = CourseOfferingStatus.OPEN;
        String search = null;

        if (arguments != null) {
            JsonNode s = arguments.get("semester");
            if (s != null && !s.isNull() && !s.asString().isBlank()) semester = s.asString();
            JsonNode st = arguments.get("status");
            if (st != null && !st.isNull() && !st.asString().isBlank()) {
                try {
                    status = CourseOfferingStatus.valueOf(st.asString().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // fall back to OPEN
                }
            }
            JsonNode q = arguments.get("search");
            if (q != null && !q.isNull() && !q.asString().isBlank()) {
                search = q.asString().toLowerCase();
            }
        }

        List<CourseOfferingDTO> offerings = courseOfferingService.list(semester, status);
        if (search != null) {
            final String needle = search;
            offerings = offerings.stream().filter(o ->
                    (o.getCode() != null && o.getCode().toLowerCase().contains(needle))
                            || (o.getTitle() != null && o.getTitle().toLowerCase().contains(needle))
            ).toList();
        }

        List<Map<String, Object>> slim = offerings.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("code", o.getCode());
            m.put("title", o.getTitle());
            m.put("semester", o.getSemester());
            m.put("credits", o.getCredits());
            m.put("prerequisites", o.getPrerequisites());
            m.put("status", o.getStatus());
            m.put("lecturers", o.getLecturerNames());
            m.put("totalCapacity", o.getTotalCapacity());
            m.put("totalEnrolled", o.getTotalEnrolled());
            m.put("seatsRemaining",
                    (o.getTotalCapacity() != null ? o.getTotalCapacity() : 0)
                            - (o.getTotalEnrolled() != null ? o.getTotalEnrolled() : 0));
            return m;
        }).toList();

        return Map.of(
                "filter", Map.of(
                        "semester", semester == null ? "ANY" : semester,
                        "status", status,
                        "search", search == null ? "" : search),
                "count", slim.size(),
                "offerings", slim);
    }
}
