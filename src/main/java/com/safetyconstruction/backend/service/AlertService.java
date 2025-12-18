package com.safetyconstruction.backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.request.alert.AiAlertRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertCreationRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertReviewRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.AlertResponse;
import com.safetyconstruction.backend.entity.*;
import com.safetyconstruction.backend.enums.AlertSeverity;
import com.safetyconstruction.backend.enums.AlertStatus;
import com.safetyconstruction.backend.enums.ViolationType;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.AlertMapper;
import com.safetyconstruction.backend.repository.*;
import com.safetyconstruction.backend.specification.AlertSpecification;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
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

        try {
            // Lấy username từ Security Context (Token)
            var context = SecurityContextHolder.getContext();
            String name = context.getAuthentication().getName();

            // Tìm user trong DB bằng username
            User reviewer =
                    userRepository.findByName(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Gán người duyệt
            alert.setReviewer(reviewer);

        } catch (Exception e) {
            // Nếu lỗi (vd: AI gọi hoặc lỗi token), có thể bỏ qua hoặc set mặc định
            System.out.println("⚠️ Không tìm thấy người duyệt từ Token: " + e.getMessage());
        }
        System.out.println("⚠️ tìm thấy người duyệt từ Token");
        //        alert.setReviewer(reviewer);
        alert.setAlertStatus(request.getAlertStatus());
        alert.setReviewNote(request.getReviewNote());

        //        alert = alertRepository.save(alert);
        return alertMapper.toAlertResponse(alertRepository.save(alert));
    }

    /**
     * ✅ REVIEW ALERT (theo ID)
     * - Giống trên, chỉ khác là không theo Project.
     */
    @Transactional
    @PreAuthorize("hasAuthority('ALERT_UPDATE')")
    public AlertResponse reviewAlert(Long alertId, AlertReviewRequest request) {
        Alert alert = alertRepository.findById(alertId).orElseThrow(() -> new AppException(ErrorCode.ALERT_NOT_FOUND));

        try {
            // Lấy username từ Security Context (Token)
            var context = SecurityContextHolder.getContext();
            String name = context.getAuthentication().getName();

            // Tìm user trong DB bằng username
            User reviewer =
                    userRepository.findByName(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Gán người duyệt
            alert.setReviewer(reviewer);

        } catch (Exception e) {
            // Nếu lỗi (vd: AI gọi hoặc lỗi token), có thể bỏ qua hoặc set mặc định
            System.out.println("⚠️ Không tìm thấy người duyệt từ Token: " + e.getMessage());
        }
        System.out.println("⚠️ tìm thấy người duyệt từ Token");
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

    private final SimpMessagingTemplate messagingTemplate; // Để gửi WebSocket

    public void processAiAlert(AiAlertRequest request) {
        // 1. Tìm Camera trong DB (Giả sử camera_id gửi lên là ID thật)
        // Nếu AI gửi string "cam1", bạn cần mapping nó với ID trong DB, ở đây giả sử AI gửi đúng ID
        Camera camera = cameraRepository
                .findById(request.getCameraId())
                .orElseThrow(() -> new RuntimeException("Camera not found ID: " + request.getCameraId()));

        // 2. Map lỗi từ AI sang Enum Java
        ViolationType type = ViolationType.UNKNOWN;
        if (request.getErrors() != null && !request.getErrors().isEmpty()) {
            if (request.getErrors().contains("no_helmet")) type = ViolationType.NO_HARD_HAT;
            else if (request.getErrors().contains("no_vest")) type = ViolationType.NO_PROTECTIVE_GEAR;
        } else if ("Human in Danger Zone".equals(request.getTitle())) {
            type = ViolationType.RESTRICTED_AREA_ENTRY;
        }

        // 3. Lưu ảnh Base64 ra file
        String imagePath = saveBase64Image(request.getImage());

        // ... Đoạn code chuyển đổi thời gian bạn đã sửa ở bước trước
        // SỬA DÒNG NÀY:
        // Cũ: "yyyy-MM-dd HH:mm:ss.SSSSSS" (Bắt buộc có đuôi)
        // Mới: "yyyy-MM-dd HH:mm:ss[.SSSSSS]" (Dấu [] nghĩa là có cũng được, không có cũng được)

        //        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
        //
        //        LocalDateTime localDateTime = LocalDateTime.parse(request.getCreatedAt(), formatter);
        Instant instant;
        if (request.getCreatedAt() != null && !request.getCreatedAt().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
                LocalDateTime localDateTime = LocalDateTime.parse(request.getCreatedAt(), formatter);
                instant = localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            } catch (Exception e) {
                // Nếu format sai thì fallback về giờ hiện tại
                log.warn("Lỗi format ngày tháng từ AI: {}, dùng giờ hiện tại.", request.getCreatedAt());
                instant = Instant.now();
            }
        } else {
            // Nếu AI không gửi createdAt -> Dùng giờ hiện tại
            instant = Instant.now();
        }

        // 4. Lưu Alert vào DB
        Alert alert = Alert.builder()
                .project(camera.getProject())
                .camera(camera)
                .type(String.valueOf(type))
                .severity(AlertSeverity.HIGH) // Hoặc parse từ request.getCreated_at()
                .imageKey(imagePath)
                .confidence(request.getConfidence() != null ? request.getConfidence() : 0.0f) // Lưu đường dẫn file
                .alertStatus(AlertStatus.NEW)
                .happenedAt(instant)
                .build();

        // ... đoạn code save alert ...
        Alert savedAlert = alertRepository.save(alert);

        String violationVi = getVietnameseMessage(savedAlert.getType());
        int confidencePercent = (int) (savedAlert.getConfidence() * 100);

        // Map từ Entity sang DTO
        AlertResponse response = AlertResponse.builder()
                .id(savedAlert.getId())
                .title("⚠️ Cảnh báo: " + violationVi)

                // Body: Chi tiết ngữ cảnh tự nhiên
                // Ví dụ: "Hệ thống phát hiện Không đội mũ bảo hộ tại Camera Tầng 5 (Độ chính xác: 94%)"
                .body(String.format(
                        "Hệ thống phát hiện %s tại khu vực %s. (Độ tin cậy: %d%%)",
                        violationVi, camera.getName(), confidencePercent))
                .type(savedAlert.getType())
                .alertStatus(savedAlert.getAlertStatus().name())
                .severity(savedAlert.getSeverity().name())
                .confidence(savedAlert.getConfidence().doubleValue())
                .imageKey(savedAlert.getImageKey())
                .happenedAt(savedAlert.getHappenedAt().toString())
                // Thông tin Camera/Project phẳng hóa
                .cameraId(camera.getId())
                .cameraName(camera.getName())
                .location(camera.getLocation())
                .projectId(camera.getProject().getId())
                .projectName(camera.getProject().getName())
                .build();

        String managerId = String.valueOf(camera.getProject().getManager().getName());

        // --- THÊM LOG ĐỂ DEBUG ---
        System.out.println(">>> WEBSOCKET DEBUG: Đang gửi tới User ID: " + managerId);
        System.out.println(
                ">>> ID User này lấy từ Project: " + camera.getProject().getName());
        // -------------------------
        //
        messagingTemplate.convertAndSend("/topic/notifications/" + managerId, response);

        //        System.out.println(">>> DEBUG: Đang gửi vào kênh TEST");
        //        messagingTemplate.convertAndSend("/topic/notifications/test", response);
    }

    private String getVietnameseMessage(String violationType) {
        if (violationType == null) return "Phát hiện vi phạm an toàn";

        switch (violationType) {
            case "NO_HARD_HAT":
                return "Không đội mũ bảo hộ";
            case "NO_PROTECTIVE_GEAR":
            case "NO_VEST": // Đề phòng Python gửi key này
                return "Thiếu áo bảo hộ/phản quang";
            case "RESTRICTED_AREA_ENTRY":
                return "Xâm nhập vùng nguy hiểm";
            case "FIRE_DETECTED":
                return "Phát hiện khói/lửa";
            default:
                return "Vi phạm không xác định (" + violationType + ")";
        }
    }

    private String saveBase64Image(String base64Str) {
        try {
            if (base64Str == null || base64Str.isEmpty()) return null;

            // Tạo thư mục uploads nếu chưa có
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);
            String fileName = "alert_" + UUID.randomUUID() + ".jpg";
            Path destinationFile = uploadDir.resolve(fileName);

            Files.write(destinationFile, decodedBytes);
            return "/images/" + fileName; // Trả về đường dẫn web (cần config ResourceHandler)
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public AlertResponse createAlertForAI(AlertCreationRequest request) {
        // 1. Tìm Camera
        var camera = cameraRepository
                .findById(request.getCameraId())
                .orElseThrow(() -> new AppException(ErrorCode.CAMERA_NOT_FOUND));
        var project = projectRepository
                .findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        // 2. Map dữ liệu
        Alert alert = alertMapper.toAlert(request, project, camera);
        alert.setCamera(camera);
        alert.setProject(camera.getProject());
        alert.setHappenedAt(java.time.Instant.now());
        // 3. Set cứng trạng thái là NEW
        alert.setAlertStatus(AlertStatus.NEW);
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                // Khởi tạo cấu hình (Bạn nên đưa các Key này vào file application.properties)
                ImageKit imageKit = ImageKit.getInstance();
                Configuration config = new Configuration(
                        "public_SWHy6fJ3e1yQm17vhcFhDXEgQig=", // Thay bằng Public Key của bạn
                        "private_N4cZOeSal1wauflawP/3Lz3p2QA=", // Thay bằng Private Key của bạn
                        "https://ik.imagekit.io/SafetyConstruction/" // Thay bằng Endpoint của bạn
                        );
                imageKit.setConfig(config);

                // Upload ảnh lên ImageKit (Hỗ trợ trực tiếp chuỗi Base64)
                FileCreateRequest fileCreateRequest =
                        new FileCreateRequest(request.getImage(), "alert_" + System.currentTimeMillis() + ".jpg");
                fileCreateRequest.setFolder("/violation_images"); // Tạo thư mục trên Cloud

                Result result = imageKit.upload(fileCreateRequest);

                // LƯU Ý: Lưu URL tuyệt đối vào cột imageKey trong Database
                alert.setImageKey(result.getUrl());

                System.out.println("✅ Đã lưu ảnh Cloud: " + result.getUrl());

            } catch (Exception e) {
                System.err.println("❌ Lỗi ImageKit: " + e.getMessage());
            }
        }
        // 4. Lưu vào DB
        // Lưu ý: Vì AI không có User login, nên trường 'createdBy' (nếu có Auditing)
        // có thể bị null. Bạn nên set cứng hoặc để trống.

        try {
            Alert savedAlert = alertRepository.save(alert);
            handleNotificationTrigger(savedAlert);
            return alertMapper.toAlertResponse(alertRepository.save(alert));
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
