package com.safetyconstruction.backend.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;

import com.safetyconstruction.backend.enums.AlertSeverity;
import com.safetyconstruction.backend.enums.AlertStatus;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    Long id;
    Long projectId;
    Long cameraId;
    String type;
    AlertSeverity severity;
    Float confidence;
    AlertStatus alertStatus;
    Instant happenedAt;
    String imageKey;
    String clipKey;
    String metadata; // raw JSON string
    String reviewerId;
    String reviewNote;
    LocalDateTime createdAt;
}
