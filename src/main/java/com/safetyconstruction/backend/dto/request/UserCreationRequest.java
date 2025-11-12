package com.safetyconstruction.backend.dto.request;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.safetyconstruction.backend.dto.request.role.RoleCreationRequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    String id;

    @Size(min = 3, max = 15, message = "USERNAME_INVALID")
    String name;

    @Email(message = "INVALID_EMAIL")
    String email;

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    boolean status;
    String locale;
    LocalDate created_at;
    LocalDate updated_at;
    Set<RoleCreationRequest> roles;
}
