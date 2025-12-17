// File: src/main/java/com/safetyconstruction/backend/controller/DashboardController.java
package com.safetyconstruction.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.DashboardResponse;
import com.safetyconstruction.backend.service.DashboardService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
    @RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    DashboardService dashboardService;

    /**
     * API duy nhất để tải tất cả dữ liệu Dashboard
     * Frontend gọi: GET /api/dashboard?projectId=1 (hoặc không có để lấy toàn cục)
     */
    @GetMapping
    public ApiResponse<DashboardResponse> getDashboardData(@ModelAttribute AlertSearchRequest searchRequest) {
        log.info("Controller: Getting dashboard data");
        DashboardResponse data = dashboardService.getDashboardData(searchRequest);
        return ApiResponse.<DashboardResponse>builder().result(data).build();
    }
}
