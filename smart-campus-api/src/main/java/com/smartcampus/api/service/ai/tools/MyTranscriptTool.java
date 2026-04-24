package com.smartcampus.api.service.ai.tools;

import com.smartcampus.api.dto.TranscriptDTO;
import com.smartcampus.api.model.Role;
import com.smartcampus.api.model.User;
import com.smartcampus.api.service.EnrollmentService;
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
public class MyTranscriptTool implements AiTool {

    private final EnrollmentService enrollmentService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_my_transcript";
    }

    @Override
    public String description() {
        return "Returns the current student's academic transcript: GPA, credits earned, courses "
                + "completed, and the list of completed courses with released grades. "
                + "Students only. Use for 'what's my GPA', 'show my transcript', 'how many credits do I have'.";
    }

    @Override public boolean isAvailableFor(User user) {
        return user.getRole() == Role.STUDENT;
    }

    @Override
    public ObjectNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        schema.putArray("required");
        return schema;
    }

    @Override
    public Object execute(ObjectNode arguments, User currentUser) {
        if (currentUser.getRole() != Role.STUDENT) {
            return Map.of("error", "not_a_student",
                    "message", "Transcripts are only available to students.");
        }

        TranscriptDTO t = enrollmentService.transcript(currentUser.getId());
        List<Map<String, Object>> completed = t.getEntries() == null ? List.of() :
                t.getEntries().stream()
                        .filter(e -> Boolean.TRUE.equals(e.getGradeReleased()))
                        .map(e -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("courseCode", e.getCourseCode());
                            m.put("courseTitle", e.getCourseTitle());
                            m.put("semester", e.getSemester());
                            m.put("credits", e.getCredits());
                            m.put("grade", e.getGradeLabel());
                            m.put("gradePoints", e.getGradePoints());
                            return m;
                        }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("gpa", t.getGpa());
        out.put("creditsEarned", t.getCreditsEarned());
        out.put("coursesCompleted", t.getCoursesCompleted());
        out.put("studentRegistrationNumber", t.getStudentRegistrationNumber());
        out.put("completedCourses", completed);
        return out;
    }
}
