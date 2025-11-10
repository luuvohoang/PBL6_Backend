// File: src/main/java/com/safetyconstruction/backend/controller/StatisticController.java
package com.safetyconstruction.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.StatisticResponse;
import com.safetyconstruction.backend.service.StatisticService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticController {

    StatisticService statisticService;

    /**
     * API để lấy thống kê theo LOẠI VI PHẠM (cho biểu đồ tròn)
     * Frontend gọi: GET /api/statistics/by-type?projectId=1&severity=HIGH
     */
    @GetMapping("/by-type")
    public ApiResponse<List<StatisticResponse>> getStatsByType(
            // Dùng @ModelAttribute để Spring tự động map các query param
            // vào đối tượng AlertSearchRequest
            @ModelAttribute AlertSearchRequest searchRequest) {
        log.info("Controller: Getting statistics by type");
        List<StatisticResponse> stats = statisticService.getStatsByType(searchRequest);
        return ApiResponse.<List<StatisticResponse>>builder().result(stats).build();
    }

    /**
     * API để lấy thống kê theo NGÀY TRONG TUẦN (cho biểu đồ cột)
     * Frontend gọi: GET /api/statistics/by-weekday?projectId=1
     */
    @GetMapping("/by-weekday")
    public ApiResponse<List<StatisticResponse>> getStatsByWeekday(@ModelAttribute AlertSearchRequest searchRequest) {
        log.info("Controller: Getting statistics by weekday");
        List<StatisticResponse> stats = statisticService.getStatsByWeekday(searchRequest);
        return ApiResponse.<List<StatisticResponse>>builder().result(stats).build();
    }

    // Bạn cũng có thể tạo một endpoint thứ 3
    // để nhóm theo cả (Type VÀ Weekday) nếu frontend của bạn cần
}
