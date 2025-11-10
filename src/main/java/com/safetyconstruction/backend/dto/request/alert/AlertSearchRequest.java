package com.safetyconstruction.backend.dto.request.alert;

import java.time.Instant;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSearchRequest {
    private Long projectId;
    private Long cameraId;
    private String type;
    private String severity;
    private String alertStatus;
    private Float minConfidence;
    private Float maxConfidence;
    private Instant happenedAfter;
    private Instant happenedBefore;
}
