package com.safetyconstruction.backend.service;

import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.request.alert.AlertCreationRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertReviewRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.AlertResponse;
import com.safetyconstruction.backend.entity.*;
import com.safetyconstruction.backend.enums.AlertStatus;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.AlertMapper;
import com.safetyconstruction.backend.repository.*;
import com.safetyconstruction.backend.specification.AlertSpecification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AlertService {

    AlertRepository alertRepository;
    ProjectRepository projectRepository;
    CameraRepository cameraRepository;
    UserRepository userRepository;
    AlertMapper alertMapper;
    NotificationService notificationService;
    RedisTemplate<String, String> redisTemplate;

    /**
     * 🧱 CREATE ALERT
     * - Chỉ ADMIN hoặc MANAGER có thể tạo alert (thủ công hoặc hệ thống).
     */
    @Transactional
    @PreAuthorize("hasAuthority('ALERT_CREATE')")
    public AlertResponse createAlert(AlertCreationRequest request) {
        log.info("Service: Creating alert type {}", request.getType());

        Project project = projectRepository
                .findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Camera camera = request.getCameraId() != null
                ? cameraRepository
                        .findById(request.getCameraId())
                        .orElseThrow(() -> new AppException(ErrorCode.CAMERA_NOT_FOUND))
                : null;

        Alert alert = alertMapper.toAlert(request, project, camera);

        try {
            Alert savedAlert = alertRepository.save(alert);
            handleNotificationTrigger(savedAlert);
            return alertMapper.toAlert(savedAlert);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    private void handleNotificationTrigger(Alert alert) {
        if (alert.getAlertStatus() != AlertStatus.NEW) {
            return;
        }

        String lockKey = String.format(
                "notify_lock:proj_%d:cam_%d:%s",
                alert.getProject().getId(),
                alert.getCamera() != null ? alert.getCamera().getId() : 0,
                alert.getType());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("Notification SPAM suppressed (Redis Debounce) for: {}", lockKey);
            return;
        }

        Specification<Alert> spec = Specification.allOf(
                AlertSpecification.withProjectId(alert.getProject().getId()),
                AlertSpecification.withCameraId(
                        alert.getCamera() != null ? alert.getCamera().getId() : null),
                AlertSpecification.withType(alert.getType()),
                AlertSpecification.withStatus("NEW"));

        long openAlertsCount = alertRepository.count(spec);
        if (openAlertsCount > 1) {
            log.warn("Notification suppressed (Status Check), user already has an open alert for: {}", lockKey);
            return;
        }

        log.info("Triggering new notification for: {}", lockKey);
        redisTemplate.opsForValue().set(lockKey, "locked", 2, TimeUnit.MINUTES);
        notificationService.createNotificationForAlert(alert);
    }

    /**
     * 🔍 GET ALERT BY ID (theo Project)
     * - Dành cho Admin, Manager, Supervisor
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ALERT_READ')")
    public AlertResponse getAlertByIdAndProjectId(Long alertId, Long projectId) {
        return alertRepository
                .findByIdAndProjectId(alertId, projectId)
                .map(alertMapper::toAlert)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));
    }

    /**
     * 📑 SEARCH ALERTS
     * - Chỉ ADMIN và MANAGER có quyền xem toàn bộ cảnh báo.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ALERT_READ', 'ALERT_MANAGE_ALL')")
    public Page<AlertResponse> searchAlerts(AlertSearchRequest searchRequest, Pageable pageable) {
        log.info("Service: Searching alerts");

        Specification<Alert> spec = Specification.allOf(
                AlertSpecification.withProjectId(searchRequest.getProjectId()),
                AlertSpecification.withCameraId(searchRequest.getCameraId()),
                AlertSpecification.withType(searchRequest.getType()),
                AlertSpecification.withSeverity(searchRequest.getSeverity()),
                AlertSpecification.withStatus(searchRequest.getAlertStatus()),
                AlertSpecification.withConfidenceRange(
                        searchRequest.getMinConfidence(), searchRequest.getMaxConfidence()),
                AlertSpecification.withHappenedTimeRange(
                        searchRequest.getHappenedAfter(), searchRequest.getHappenedBefore()));

        return alertRepository.findAll(spec, pageable).map(alertMapper::toAlert);
    }

    /**
     * ✅ REVIEW ALERT (theo Project)
     * - Chỉ Admin, Manager, Supervisor có thể duyệt.
     */
    @Transactional
    @PreAuthorize("hasAuthority('ALERT_UPDATE')")
    public AlertResponse reviewAlert(Long alertId, Long projectId, AlertReviewRequest request) {
        Alert alert = alertRepository
                .findByIdAndProjectId(alertId, projectId)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));

        User reviewer = userRepository
                .findById(request.getReviewerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        alert.setReviewer(reviewer);
        alert.setAlertStatus(request.getAlertStatus());
        alert.setReviewNote(request.getReviewNote());

        alert = alertRepository.save(alert);
        return alertMapper.toAlert(alert);
    }

    /**
     * ✅ REVIEW ALERT (theo ID)
     * - Giống trên, chỉ khác là không theo Project.
     */
    @Transactional
    @PreAuthorize("hasAuthority('ALERT_UPDATE')")
    public AlertResponse reviewAlert(Long alertId, AlertReviewRequest request) {
        Alert alert = alertRepository.findById(alertId).orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));

        User reviewer = userRepository
                .findById(request.getReviewerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        alert.setReviewer(reviewer);
        alert.setAlertStatus(request.getAlertStatus());
        alert.setReviewNote(request.getReviewNote());

        alert = alertRepository.save(alert);
        return alertMapper.toAlert(alert);
    }

    /**
     * 🔎 GET ALERT BY ID
     * - Mọi người có thể xem alert thuộc dự án của mình, nhưng
     *   chỉ ADMIN có thể xem alert của dự án khác.
     */
    @Transactional(readOnly = true)
    @PostAuthorize("hasAuthority('ALERT_READ')")
    public AlertResponse getAlertById(Long alertId) {
        log.info("Service: Getting alert id {}", alertId);
        return alertRepository
                .findById(alertId)
                .map(alertMapper::toAlert)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));
    }

    /**
     * ❌ DELETE ALERT
     * - Chỉ ADMIN mới có thể xóa.
     */
    @Transactional
    @PreAuthorize("hasAuthority('ALERT_DELETE')")
    public void deleteAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId).orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));
        alertRepository.delete(alert);
        log.info("Deleted alert id {}", alertId);
    }
}
