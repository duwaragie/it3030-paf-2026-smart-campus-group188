package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.CourseSectionDTO;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.CourseSectionService;
import com.smartcampus.api.service.ai.AiTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListCourseSectionsTool implements AiTool {

    private final CourseSectionService courseSectionService;
    private final ObjectMapper objectMapper;

    @Override public String name() { return "list_course_sections"; }

    @Override public String description() {
        return "Lists all sections of a course offering (with section ID, label, lecturer, "
                + "capacity, seats available). Use this to find the sectionId before calling enroll_in_section.";
    }

    @Override public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode id = props.putObject("offeringId");
        id.put("type", "integer");
        schema.putArray("required").add("offeringId");
        return schema;
    }

    @Override public Object execute(ObjectNode arguments, User currentUser) {
        if (arguments == null) return Map.of("error", "missing_arguments");
        long offeringId = arguments.path("offeringId").asLong(0);
        if (offeringId <= 0) return Map.of("error", "missing_offeringId");
        try {
            List<CourseSectionDTO> sections = courseSectionService.listByOffering(offeringId);
            List<Map<String, Object>> slim = sections.stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sectionId", s.getId());
                m.put("label", s.getLabel());
                m.put("courseCode", s.getCourseCode());
                m.put("courseTitle", s.getCourseTitle());
                m.put("semester", s.getSemester());
                m.put("credits", s.getCredits());
                m.put("lecturer", s.getLecturerName());
                m.put("capacity", s.getCapacity());
                m.put("enrolledCount", s.getEnrolledCount());
                m.put("seatsAvailable", s.getSeatsAvailable());
                return m;
            }).toList();
            return Map.of("offeringId", offeringId, "count", slim.size(), "sections", slim);
        } catch (Exception e) {
            return Map.of("error", "list_failed", "message", e.getMessage());
        }
    }
}
