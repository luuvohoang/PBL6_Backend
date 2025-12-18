package com.safetyconstruction.backend.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.alert.AlertCreationRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertReviewRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.AlertResponse;
import com.safetyconstruction.backend.service.AlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts") // Base URL chung cho toàn bộ controller này
@RequiredArgsConstructor
public class GlobalAlertController {

    private final AlertService alertService;

    /**
     * 1. API TÌM KIẾM & LẤY DANH SÁCH
     * Frontend gọi:
     * - axios.get('/alerts') -> Lấy tất cả (mặc định 20 cái mới nhất)
     * - axios.get('/alerts', { params: { projectId: 1, alertStatus: 'NEW' } }) -> Lọc theo project
     */
    @GetMapping
    public ApiResponse<Page<AlertResponse>> searchAlerts(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long cameraId,
            @RequestParam(required = false) String alertStatus,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String happenedAfter, // <--- Cái này có thể NULL
            @RequestParam(required = false) String happenedBefore, // <--- Cái này có thể NULL
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AlertSearchRequest searchRequest = new AlertSearchRequest();
        searchRequest.setProjectId(projectId);
        searchRequest.setCameraId(cameraId);
        searchRequest.setAlertStatus(alertStatus);
        searchRequest.setType(type);

        // --- SỬA LỖI TẠI ĐÂY ---
        // Đừng dùng Instant.parse() ở đây nếu DTO của bạn nhận String.
        // Chỉ cần truyền thẳng String vào, Service sẽ lo phần còn lại.
        if (happenedAfter != null && !happenedAfter.isEmpty()) {
            try {
                // Frontend phải gửi format chuẩn ISO-8601 (VD: 2025-12-18T10:15:30Z)
                searchRequest.setHappenedAfter(Instant.parse(happenedAfter));
            } catch (Exception e) {
                // Nếu Frontend gửi sai format, code sẽ không chết mà chỉ in lỗi ra log
                System.err.println("Lỗi format ngày tháng (After): " + happenedAfter);
            }
        }

        // 2. Xử lý happenedBefore (Từ String -> Instant)
        if (happenedBefore != null && !happenedBefore.isEmpty()) {
            try {
                searchRequest.setHappenedBefore(Instant.parse(happenedBefore));
            } catch (Exception e) {
                System.err.println("Lỗi format ngày tháng (Before): " + happenedBefore);
            }
        }
        // -----------------------

        /* LƯU Ý: Nếu DTO AlertSearchRequest của bạn bắt buộc field là Instant (không phải String),
        thì bạn phải kiểm tra null trước khi parse:

        if (happenedAfter != null && !happenedAfter.isEmpty()) {
        	// searchRequest.setHappenedAfter(Instant.parse(happenedAfter));
        }

        Tuy nhiên, tốt nhất là để DTO nhận String để tránh lỗi này.
        */

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ApiResponse.<Page<AlertResponse>>builder()
                .code(1000)
                .result(alertService.searchAlerts(searchRequest, pageable))
                .build();
    }

    /**
     * 2. API DUYỆT ALERT (REVIEW)
     * Frontend gọi: axios.put('/alerts/123/review', { alertStatus: 'RESOLVED', ... })
     */
    @PutMapping("/{alertId}/review")
    public ApiResponse<AlertResponse> reviewAlert(@PathVariable Long alertId, @RequestBody AlertReviewRequest request) {
        // Gọi service xử lý logic duyệt
        return ApiResponse.<AlertResponse>builder()
                .code(1000)
                .result(alertService.reviewAlert(alertId, request))
                .message("Đã cập nhật trạng thái thành công!")
                .build();
    }

    /**
     * 3. (Optional) API LẤY CHI TIẾT 1 ALERT
     * Frontend gọi: axios.get('/alerts/123')
     */
    @GetMapping("/{alertId}")
    public ApiResponse<AlertResponse> getAlertDetail(@PathVariable Long alertId) {
        return ApiResponse.<AlertResponse>builder()
                .code(1000)
                .result(alertService.getAlertById(alertId))
                .build();
    }

    @PostMapping("/internal")
    public ApiResponse<AlertResponse> createAlertFromAI(@RequestBody AlertCreationRequest request) {
        // Gọi service tạo alert (Bạn dùng lại hàm createAlert trong service cũ)
        return ApiResponse.<AlertResponse>builder()
                .code(1000)
                .result(alertService.createAlertForAI(request))
                .build();
    }
}
