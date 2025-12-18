package com.safetyconstruction.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    private Long id;
    private String title;
    private String body;
    private String type;
    private String alertStatus;
    private String severity;
    private Double confidence;
    private String imageKey;
    private String happenedAt;

    // Chỉ lấy tên hoặc ID, không lấy cả object để tránh vòng lặp
    private Long cameraId;
    private String cameraName;
    private String location;
    private Long projectId;
    private String projectName;
}
