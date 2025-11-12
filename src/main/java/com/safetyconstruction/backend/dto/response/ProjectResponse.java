package com.safetyconstruction.backend.dto.response;

import java.time.LocalDate;
import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    LocalDate created_at;
    UserResponse manager;
    Set<CameraResponse> cameras;
}
