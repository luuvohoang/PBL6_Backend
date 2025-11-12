package com.safetyconstruction.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.safetyconstruction.backend.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p WHERE " + "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Project> searchProjects(String name, String location, String status, Pageable pageable);

    Page<Project> findByManagerId(String id, Pageable pageable);
    //    Page<Project> findByManagerName(String managerName, Pageable pageable);

    boolean existsByIdAndManagerName(Long projectId, String username);
}
