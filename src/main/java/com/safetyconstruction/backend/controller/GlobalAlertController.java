package com.safetyconstruction.backend.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.alert.*;
import com.safetyconstruction.backend.dto.response.AlertResponse;
import com.safetyconstruction.backend.service.AlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class GlobalAlertController {
    private final AlertService alertService;

    @PostMapping
    public ApiResponse<AlertResponse> createAlert(@Valid @RequestBody AlertCreationRequest request) {
        return ApiResponse.<AlertResponse>builder()
                .result(alertService.createAlert(request))
                .message("Alert created successfully")
                .build();
    }

    @GetMapping
    public ApiResponse<Page<AlertResponse>> searchAlerts(AlertSearchRequest searchRequest, Pageable pageable) {
        return ApiResponse.<Page<AlertResponse>>builder()
                .result(alertService.searchAlerts(searchRequest, pageable))
                .build();
    }

    @GetMapping("/{alertId}")
    public ApiResponse<AlertResponse> getAlert(@PathVariable Long alertId) {
        return ApiResponse.<AlertResponse>builder()
                .result(alertService.getAlertById(alertId))
                .build();
    }

    @PatchMapping("/{alertId}/review")
    public ApiResponse<AlertResponse> reviewAlert(
            @PathVariable Long alertId, @Valid @RequestBody AlertReviewRequest request) {
        return ApiResponse.<AlertResponse>builder()
                .result(alertService.reviewAlert(alertId, request))
                .message("Alert reviewed successfully")
                .build();
    }
}
