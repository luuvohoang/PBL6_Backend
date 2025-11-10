package com.safetyconstruction.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.response.NotificationResponse;
import com.safetyconstruction.backend.service.NotificationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor // Thêm
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // Thêm
@Slf4j // Thêm
public class NotificationController {

    NotificationService notificationService; // Xóa constructor

    /**
     * Lấy thông báo cho ADMIN (có thể lọc theo user)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<NotificationResponse>> listAllNotifications(
            @RequestParam(required = false) String userId, Pageable pageable) {

        log.info("Controller: Admin listing notifications");
        return ApiResponse.<Page<NotificationResponse>>builder()
                .result(notificationService.listAll(userId, pageable))
                .build();
    }

    /**
     * Lấy thông báo cho người dùng đã đăng nhập (USER, MANAGER)
     */
    @GetMapping("/my-notifications")
    public ApiResponse<Page<NotificationResponse>> listMyNotifications(Pageable pageable) {
        log.info("Controller: Listing 'my' notifications");
        return ApiResponse.<Page<NotificationResponse>>builder()
                .result(notificationService.listMyNotifications(pageable))
                .build();
    }

    /**
     * Đánh dấu thông báo là đã đọc
     */
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable Long notificationId) {

        log.info("Controller: Marking notification {} as read", notificationId);
        return ApiResponse.<NotificationResponse>builder()
                .result(notificationService.markAsRead(notificationId))
                .message("Notification marked as read")
                .build();
    }

    @PatchMapping("/read-all")
    public ApiResponse<String> markAllAsRead() {
        log.info("Controller: Marking all notifications as read");
        notificationService.markAllAsRead();
        return ApiResponse.<String>builder()
                .message("All notifications marked as read")
                .build();
    }
}
