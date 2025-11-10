package com.safetyconstruction.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.safetyconstruction.backend.entity.Camera;

public interface CameraRepository extends JpaRepository<Camera, Long>, JpaSpecificationExecutor<Camera> {
    // Page<Camera> findByNameContainingIgnoreCaseAndLocationContainingIgnoreCase(String name, String location, Pageable
    // pageable);
}
