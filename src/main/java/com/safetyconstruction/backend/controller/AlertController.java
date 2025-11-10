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
@RequestMapping("/api/projects/{projectId}/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public ApiResponse<AlertResponse> createAlertForProject(
            @PathVariable Long projectId, @Valid @RequestBody AlertCreationRequest request) {

        // Ghi đè projectId từ URL để đảm bảo request hợp lệ
        request.setProjectId(projectId);

        return ApiResponse.<AlertResponse>builder()
                .result(alertService.createAlert(request))
                .message("Alert created successfully for project " + projectId)
                .build();
    }

    /**
     * TÌM KIẾM Alert TRONG Project này.
     * Tái sử dụng logic 'searchAlerts' của bạn.
     */
    @GetMapping
    public ApiResponse<Page<AlertResponse>> searchAlertsInProject(
            @PathVariable Long projectId,
            AlertSearchRequest searchRequest, // Spring sẽ map các query params (ví dụ: ?type=NO_HELMET)
            Pageable pageable) {

        // Ghi đè projectId từ URL để đảm bảo tìm kiếm chỉ trong project này
        searchRequest.setProjectId(projectId);

        return ApiResponse.<Page<AlertResponse>>builder()
                .result(alertService.searchAlerts(searchRequest, pageable))
                .build();
    }

    /**
     * LẤY Alert cụ thể TỪ Project này.
     * Sử dụng phương thức service mới để bảo mật.
     */
    @GetMapping("/{alertId}")
    public ApiResponse<AlertResponse> getAlertFromProject(@PathVariable Long projectId, @PathVariable Long alertId) {

        // Gọi phương thức service mới, có kiểm tra cả projectId
        return ApiResponse.<AlertResponse>builder()
                .result(alertService.getAlertByIdAndProjectId(alertId, projectId))
                .build();
    }

    /**
     * REVIEW Alert cụ thể TỪ Project này.
     * Sử dụng phương thức service mới để bảo mật.
     */
    @PatchMapping("/{alertId}/review")
    public ApiResponse<AlertResponse> reviewAlertInProject(
            @PathVariable Long projectId, @PathVariable Long alertId, @Valid @RequestBody AlertReviewRequest request) {

        // Gọi phương thức service mới, có kiểm tra cả projectId
        return ApiResponse.<AlertResponse>builder()
                .result(alertService.reviewAlert(alertId, projectId, request))
                .message("Alert reviewed successfully")
                .build();
    }
}
