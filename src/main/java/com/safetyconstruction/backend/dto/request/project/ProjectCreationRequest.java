package com.safetyconstruction.backend.dto.request.project;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import com.safetyconstruction.backend.dto.request.camera.CameraCreationRequest;
import com.safetyconstruction.backend.entity.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectCreationRequest {
    Long id;

    @Size(min = 3, max = 100, message = "NAME_SIZE")
    String name;

    String description;

    LocalDate created_at;

    User owner;

    Set<CameraCreationRequest> cameras;
}
