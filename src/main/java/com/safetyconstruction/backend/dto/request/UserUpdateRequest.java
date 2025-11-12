package com.safetyconstruction.backend.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Email;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @Email(message = "INVALID_EMAIL")
    String email;

    String password;

    boolean status;
    String locale;
    LocalDate created_at;
    LocalDate updated_at;
    List<String> roles;
}
