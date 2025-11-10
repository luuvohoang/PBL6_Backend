package com.safetyconstruction.backend.specification;

import org.springframework.data.jpa.domain.Specification;

import com.safetyconstruction.backend.entity.Camera;

public class CameraSpecification {

    /**
     * Private constructor để ngăn việc khởi tạo class tiện ích này
     */
    private CameraSpecification() {}

    /**
     * Trả về bộ lọc theo projectId
     */
    public static Specification<Camera> byProjectId(Long projectId) {
        // Luôn yêu cầu projectId, không trả về null
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    /**
     * Trả về bộ lọc theo tên (bỏ qua nếu name là null/trống)
     */
    public static Specification<Camera> byName(String name) {
        if (name == null || name.isBlank()) {
            return null; // Specification.allOf() sẽ tự động bỏ qua
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Trả về bộ lọc theo vị trí (bỏ qua nếu location là null/trống)
     */
    public static Specification<Camera> byLocation(String location) {
        if (location == null || location.isBlank()) {
            return null; // Specification.allOf() sẽ tự động bỏ qua
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }
}
