package com.safetyconstruction.backend.dto.request.alert;

import java.util.List;

import com.safetyconstruction.backend.enums.AlertStatus;

import lombok.Data;

@Data
public class AiAlertRequest {
    private Long cameraId; // Khớp với JSON từ Flask
    private List<String> errors; // ["no_helmet", "no_vest"]
    private String image; // Base64 string
    private String createdAt;
    private String title;
    private AlertStatus alertStatus;
    private Float confidence; // Cho API danger_zone
}
