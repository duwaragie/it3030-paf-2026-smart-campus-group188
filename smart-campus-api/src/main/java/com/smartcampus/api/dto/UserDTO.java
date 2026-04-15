package com.smartcampus.api.dto;

import com.smartcampus.api.model.AuthProvider;
import com.smartcampus.api.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String picture;
    private Role role;
    private AuthProvider authProvider;
    private boolean emailVerified;
    private String studentRegistrationNumber;
    private String employeeId;
    private boolean profileComplete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
