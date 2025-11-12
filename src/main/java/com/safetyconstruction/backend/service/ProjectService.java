package com.safetyconstruction.backend.service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.request.project.ProjectCreationRequest;
import com.safetyconstruction.backend.dto.request.project.ProjectUpdateRequest;
import com.safetyconstruction.backend.dto.response.ProjectResponse;
import com.safetyconstruction.backend.entity.Camera;
import com.safetyconstruction.backend.entity.Project;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.CameraMapper;
import com.safetyconstruction.backend.mapper.ProjectMapper;
import com.safetyconstruction.backend.repository.ProjectRepository;
import com.safetyconstruction.backend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectService {

    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMapper projectMapper;
    CameraMapper cameraMapper;

    /**
     * Tạo project
     * - ADMIN: tạo cho bất kỳ ai
     * - MANAGER: tạo cho chính mình (được tự động gán manager)
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProjectResponse create(ProjectCreationRequest request) {
        log.info("Service: Creating project");

        Project project = projectMapper.toProject(request);

        Set<Camera> cameras = new HashSet<>();
        if (request.getCameras() != null && !request.getCameras().isEmpty()) {
            cameras = request.getCameras().stream()
                    .map(cameraReq -> {
                        Camera camera = cameraMapper.toCamera(cameraReq);
                        camera.setProject(project);
                        return camera;
                    })
                    .collect(Collectors.toSet());
        }
        project.setCameras(cameras);

        // Lấy user hiện tại
        String currentUsername =
                SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository
                .findByName(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        project.setManager(currentUser);

        try {
            Project savedProject = projectRepository.save(project);
            return projectMapper.toProjectResponse(savedProject);
        } catch (DataIntegrityViolationException ex) {
            handleDataIntegrityException(ex);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * Update project
     * - Chỉ ADMIN mới được update
     */
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        log.info("Service: Updating project id {}", id);
        Project project =
                projectRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        // Cập nhật manager nếu có managerId trong request
        if (request.getManagerId() != null) {
            User newManager = userRepository
                    .findById(request.getManagerId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            project.setManager(newManager);
            log.info("Updated manager to: {}", newManager.getName());
        }

        projectMapper.updateProject(project, request);
        Project savedProject = projectRepository.save(project);
        ProjectResponse response = projectMapper.toProjectResponse(savedProject);
        log.info(
                "Returning project response with managerName: {}",
                response.getManager().getName());
        return response;
    }

    /**
     * Xóa project
     * - Chỉ ADMIN mới được xóa
     */
    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    public void delete(Long id) {
        log.info("Service: Deleting project id {}", id);
        Project project =
                projectRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        projectRepository.delete(project);
    }

    /**
     * Lấy danh sách project
     * - ADMIN: xem tất cả
     * - MANAGER: xem project của chính mình
     */
    @PreAuthorize("hasAuthority('PROJECT_READ')") // Cần quyền 'PROJECT_READ'
    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Pageable pageable) {
        User currentUser = getCurrentUser();

        Page<Project> projects;
        boolean isAdmin =
                currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ADMIN"));

        if (isAdmin) {
            projects = projectRepository.findAll(pageable);
        } else {
            projects = projectRepository.findByManagerId(currentUser.getId(), pageable);
        }

        return projects.map(projectMapper::toProjectResponse);
    }

    /**
     * Lấy project theo id
     * - ADMIN: xem tất cả
     * - MANAGER: chỉ xem project của chính mình
     */
    @PostAuthorize("returnObject.managerName == authentication.name or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        log.info("Service: Get project by id {}", id);
        Project project =
                projectRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        return projectMapper.toProjectResponse(project);
    }

    // --- private helpers ---
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByName(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    public boolean isProjectManager(Long projectId, String username) {
        return projectRepository.existsByIdAndManagerName(projectId, username);
    }

    private void handleDataIntegrityException(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getRootCause();
        String errorMessage = null;

        if (rootCause instanceof ConstraintViolationException) {
            errorMessage =
                    ((ConstraintViolationException) rootCause).getSQLException().getMessage();
        } else if (rootCause instanceof SQLIntegrityConstraintViolationException) {
            errorMessage = ((SQLIntegrityConstraintViolationException) rootCause).getMessage();
        }

        if (errorMessage != null && errorMessage.contains("project.name")) {
            throw new AppException(ErrorCode.PROJECT_NAME_EXISTED);
        }
    }
}
