package com.safetyconstruction.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.safetyconstruction.backend.entity.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
    Optional<Alert> findByIdAndProjectId(Long id, Long projectId);

    Page<Alert> findByProjectIdOrderByHappenedAtDesc(Long projectId, Pageable pageable);
}
