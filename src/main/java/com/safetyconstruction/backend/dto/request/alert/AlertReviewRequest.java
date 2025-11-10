package com.safetyconstruction.backend.dto.request.alert;

import jakarta.validation.constraints.*;

import com.safetyconstruction.backend.enums.AlertStatus;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertReviewRequest {
    @NotNull
    String reviewerId;

    @NotNull
    AlertStatus alertStatus;

    @Size(max = 2000)
    String reviewNote;
}
