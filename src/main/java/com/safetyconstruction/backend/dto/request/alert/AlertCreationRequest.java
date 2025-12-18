package com.safetyconstruction.backend.dto.request.alert;

import java.time.Instant;

import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertCreationRequest {
    @NotNull
    Long projectId;

    Long cameraId;

    @NotBlank
    @Size(max = 100)
    String type;

    @NotNull
    String severity; // use enum names: LOW, MEDIUM, HIGH, CRITICAL

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    Float confidence;

    @NotNull
    Instant happenedAt;

    @Size(max = 1024)
    String imageKey;

    private String image;

    @Size(max = 1024)
    String clipKey;

    // raw JSON string; validate size only
    @Size(max = 20000)
    String metadata;
}
