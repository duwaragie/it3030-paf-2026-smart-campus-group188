package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.CourseOfferingDTO;
import com.smartcampus.api.dto.EnrollmentDTO;
import com.smartcampus.api.model.EnrollmentStatus;
import com.smartcampus.api.model.Role;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.CourseOfferingService;
import com.smartcampus.api.service.EnrollmentService;
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
public class MyEnrollmentsTool implements AiTool {

    private final EnrollmentService enrollmentService;
    private final CourseOfferingService courseOfferingService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_my_courses";
    }

    @Override
    public String description() {
        return "Returns the current user's courses. For STUDENTS: their enrollments (current + past) "
                + "with status, section, lecturer, and released grades. For LECTURERS: the course "
                + "offerings they teach. Use for 'what courses am I taking/teaching', 'what grade did "
                + "I get for X', 'am I still enrolled in Y'. Optional status filter for students.";
    }

    @Override
    public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode status = props.putObject("status");
        status.put("type", "string");
        status.put("description", "Optional status filter (STUDENT only): ENROLLED, WAITLISTED, COMPLETED, WITHDRAWN");
        status.putArray("enum")
                .add("ENROLLED").add("WAITLISTED").add("COMPLETED").add("WITHDRAWN");
        schema.putArray("required");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        Role role = currentUser.getRole();
        if (role == Role.LECTURER) {
            List<CourseOfferingDTO> offerings = courseOfferingService.listByLecturer(currentUser.getId());
            List<Map<String, Object>> slim = offerings.stream().map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", o.getId());
                m.put("code", o.getCode());
                m.put("title", o.getTitle());
                m.put("semester", o.getSemester());
                m.put("credits", o.getCredits());
                m.put("status", o.getStatus());
                m.put("totalCapacity", o.getTotalCapacity());
                m.put("totalEnrolled", o.getTotalEnrolled());
                return m;
            }).toList();
            return Map.of("role", "LECTURER", "count", slim.size(), "courses", slim);
        }

        if (role != Role.STUDENT) {
            return Map.of(
                    "role", role,
                    "count", 0,
                    "message", "Only students and lecturers have personal course data. "
                            + "Try browse_course_offerings for the full catalogue.");
        }

        List<EnrollmentDTO> list = enrollmentService.listMine(currentUser.getId());
        JsonNode statusNode = arguments == null ? null : arguments.get("status");
        if (statusNode != null && !statusNode.isNull()) {
            try {
                EnrollmentStatus filter = EnrollmentStatus.valueOf(statusNode.asString().toUpperCase());
                list = list.stream().filter(e -> e.getStatus() == filter).toList();
            } catch (IllegalArgumentException ignored) {
                // invalid status — return unfiltered
            }
        }

        List<Map<String, Object>> slim = list.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", e.getId());
            m.put("courseCode", e.getCourseCode());
            m.put("courseTitle", e.getCourseTitle());
            m.put("semester", e.getSemester());
            m.put("credits", e.getCredits());
            m.put("section", e.getSectionLabel());
            m.put("lecturer", e.getLecturerName());
            m.put("status", e.getStatus());
            // Grades are ONLY surfaced if released — mirrors /transcript behavior.
            if (Boolean.TRUE.equals(e.getGradeReleased())) {
                m.put("grade", e.getGradeLabel());
                m.put("gradePoints", e.getGradePoints());
            } else {
                m.put("grade", null);
                m.put("gradeNote", "Not yet released");
            }
            m.put("enrolledAt", e.getEnrolledAt());
            return m;
        }).toList();

        return Map.of("role", "STUDENT", "count", slim.size(), "enrollments", slim);
    }
}
