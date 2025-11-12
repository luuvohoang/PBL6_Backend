package com.safetyconstruction.backend.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.project.ProjectCreationRequest;
import com.safetyconstruction.backend.dto.request.project.ProjectUpdateRequest;
import com.safetyconstruction.backend.dto.response.ProjectResponse;
import com.safetyconstruction.backend.service.ProjectService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProjectController {

    ProjectService projectService;

    /**
     * Tạo project
     * - Chỉ ADMIN hoặc MANAGER mới có quyền tạo
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreationRequest request) {
        log.info("Controller: Creating project '{}'", request.getName());
        return ApiResponse.<ProjectResponse>builder()
                .message("Project created successfully")
                .result(projectService.create(request))
                .build();
    }

    /**
     * Lấy danh sách project
     * - ADMIN: xem tất cả
     * - MANAGER: xem project của chính mình
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PROJECT_READ')")
    public ApiResponse<Page<ProjectResponse>> list(Pageable pageable) {
        log.info("Controller: Listing projects");
        return ApiResponse.<Page<ProjectResponse>>builder()
                .result(projectService.list(pageable))
                .build();
    }

    /**
     * Lấy project theo ID
     * - ADMIN: xem tất cả
     * - MANAGER: chỉ xem project do mình quản lý
     */
    @GetMapping("/{id}")
    @PostAuthorize("returnObject.result.manager.name == authentication.name or hasAuthority('ADMIN')")
    public ApiResponse<ProjectResponse> getById(@PathVariable Long id) {
        log.info("Controller: Getting project id {}", id);
        return ApiResponse.<ProjectResponse>builder()
                .result(projectService.findById(id))
                .build();
    }

    /**
     * Cập nhật project
     * - ADMIN: có thể cập nhật bất kỳ project nào
     * - MANAGER: chỉ có thể cập nhật project do mình quản lý
     */
    @PutMapping("/{id}")
    //    @PreAuthorize("hasAuthority('PROJECT_UPDATE') and (hasAuthority('ADMIN') or
    // @projectService.isProjectManager(#id, authentication.name))")
    public ApiResponse<ProjectResponse> update(
            @PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        log.info("Controller: Updating project id {}", id);
        return ApiResponse.<ProjectResponse>builder()
                .message("Project updated successfully")
                .result(projectService.update(id, request))
                .build();
    }

    /**
     * Xóa project
     * - Chỉ ADMIN mới có quyền xóa
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        log.info("Controller: Deleting project id {}", id);
        projectService.delete(id);
        return ApiResponse.<String>builder()
                .message("Project deleted successfully")
                .result("Project with id " + id + " has been deleted.")
                .build();
    }
}
