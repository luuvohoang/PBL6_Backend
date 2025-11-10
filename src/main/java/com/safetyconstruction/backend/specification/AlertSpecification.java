package com.safetyconstruction.backend.specification;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.safetyconstruction.backend.entity.Alert;

public class AlertSpecification {
    public static Specification<Alert> withProjectId(Long projectId) {
        return (root, query, cb) -> {
            if (projectId == null) return null;
            return cb.equal(root.get("project").get("id"), projectId);
        };
    }

    public static Specification<Alert> withCameraId(Long cameraId) {
        return (root, query, cb) -> {
            if (cameraId == null) return null;
            return cb.equal(root.get("camera").get("id"), cameraId);
        };
    }

    public static Specification<Alert> withType(String type) {
        return (root, query, cb) -> {
            if (type == null || type.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("type")), "%" + type.toLowerCase() + "%");
        };
    }

    public static Specification<Alert> withSeverity(String severity) {
        return (root, query, cb) -> {
            if (severity == null || severity.trim().isEmpty()) return null;
            return cb.equal(root.get("severity").as(String.class), severity.toUpperCase());
        };
    }

    public static Specification<Alert> withStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isEmpty()) return null;

            // SỬA LẠI: Tên trường đúng trong Entity là "alertStatus"
            return cb.equal(root.get("alertStatus").as(String.class), status.toUpperCase());
        };
    }

    public static Specification<Alert> withConfidenceRange(Float min, Float max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("confidence"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("confidence"), min);
            return cb.between(root.get("confidence"), min, max);
        };
    }

    public static Specification<Alert> withHappenedTimeRange(Instant after, Instant before) {
        return (root, query, cb) -> {
            if (after == null && before == null) return null;
            if (after == null) return cb.lessThanOrEqualTo(root.get("happenedAt"), before);
            if (before == null) return cb.greaterThanOrEqualTo(root.get("happenedAt"), after);
            return cb.between(root.get("happenedAt"), after, before);
        };
    }
}
