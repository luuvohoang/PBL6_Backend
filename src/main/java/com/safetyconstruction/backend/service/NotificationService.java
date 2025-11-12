package com.safetyconstruction.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.response.NotificationResponse;
import com.safetyconstruction.backend.entity.Alert;
import com.safetyconstruction.backend.entity.Notification;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.NotificationMapper;
import com.safetyconstruction.backend.repository.AlertRepository;
import com.safetyconstruction.backend.repository.NotificationRepository;
import com.safetyconstruction.backend.repository.RoleRepository;
import com.safetyconstruction.backend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j // Thêm
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // Thêm
public class NotificationService {

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    AlertRepository alertRepository;
    NotificationMapper mapper;
    RoleRepository roleRepository;

    SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createNotificationForAlert(Alert alert) {
        log.info("Service: Creating notification for Alert ID: {}", alert.getId());

        // TODO: Logic tìm người nhận (hiện đang gửi cho tất cả ADMIN)
        List<User> usersToNotify = userRepository.findAllByRole(
                Optional.ofNullable(Role.builder().name("ADMIN").build()) // (Giả sử bạn dùng Role Entity)
                );

        String title = String.format("Cảnh báo %s: %s", alert.getSeverity(), alert.getType());
        String body = String.format(
                "Phát hiện vi phạm tại camera '%s' (Dự án: %s)",
                alert.getCamera() != null ? alert.getCamera().getName() : "Không rõ",
                alert.getProject().getName());

        for (User user : usersToNotify) {
            Notification notification = Notification.builder()
                    .user(user)
                    .alert(alert)
                    .title(title)
                    .body(body)
                    .read(false)
                    .build();
            Notification savedNotification = notificationRepository.save(notification);

            // --- BƯỚC QUAN TRỌNG ---
            // 1. Chuyển đổi sang DTO
            NotificationResponse dto = mapper.toNotificationResponse(savedNotification);

            // 2. Lấy ID của người nhận
            String userChannel = user.getName(); // <-- SỬA LÀM SAO LẤY ĐƯỢC USERNAME

            log.info("Pushing WebSocket notification to /topic/notifications/{}", userChannel);
            messagingTemplate.convertAndSend("/topic/notifications/" + userChannel, dto);
            // --- HẾT ---
        }
    }
    /**
     * Phương thức này được gọi nội bộ (ví dụ: bởi AlertService)
     */
    public NotificationResponse create(String userId, Long alertId, String title, String body) {
        log.info("Service: Creating notification for user {}", userId);
        User user = userRepository
                .findById(userId)
                // Sửa: Dùng AppException
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Alert alert = null;
        if (alertId != null) {
            alert = alertRepository
                    .findById(alertId)
                    // Sửa: Dùng AppException
                    .orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));
        }

        Notification notification = Notification.builder()
                .user(user)
                .alert(alert)
                .title(title)
                .body(body)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapper.toNotificationResponse(saved);
    }

    /**
     * Lấy danh sách thông báo (cho Admin, có thể lọc theo userId)
     */
    @PreAuthorize("hasRole('ADMIN')") // Bảo mật
    public Page<NotificationResponse> listAll(String username, Pageable pageable) { // <-- Đổi tên param cho rõ
        log.info("Service: Admin listing notifications, filter by user: {}", username);
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending());
        }

        // --- SỬA LỖI Ở ĐÂY ---
        Specification<Notification> spec = Specification.allOf(byUsername(username)); // <-- Sửa

        return notificationRepository.findAll(spec, pageable).map(mapper::toNotificationResponse);
    }

    // Giả sử bạn đã inject UserRepository
    // @Autowired
    // private UserRepository userRepository;

    @Cacheable("notifications") // (Giả sử tên cache là "notifications")
    public Page<NotificationResponse> listMyNotifications(Pageable pageable) {
        String currentUsername =
                SecurityContextHolder.getContext().getAuthentication().getName();
        Specification<Notification> spec = Specification.allOf(byUsername(currentUsername));
        return notificationRepository.findAll(spec, pageable).map(mapper::toNotificationResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = "notifications", allEntries = true)
    public NotificationResponse markAsRead(Long notificationId) {
        String currentUsername =
                SecurityContextHolder.getContext().getAuthentication().getName();

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getName().equals(currentUsername)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapper.toNotificationResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "notifications", allEntries = true)
    public void markAllAsRead() {
        // 1. Lấy TÊN đăng nhập
        String currentUsername =
                SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Service: Marking all as read for user {}", currentUsername);

        // 2. Dùng 'byUsername' (thay vì byUserId)
        Specification<Notification> unreadSpec =
                Specification.allOf(byUsername(currentUsername), (root, query, cb) -> cb.isFalse(root.get("read")));

        List<Notification> unreadNotifications = notificationRepository.findAll(unreadSpec);

        if (unreadNotifications.isEmpty()) return;

        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    private Specification<Notification> byUsername(String username) {
        return (root, query, cb) -> (username == null || username.isBlank())
                ? null
                : cb.equal(root.get("user").get("name"), username); // So sánh TÊN (Name)
    }
}
