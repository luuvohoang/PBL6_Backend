package com.safetyconstruction.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.safetyconstruction.backend.dto.request.camera.CameraCreationRequest;
import com.safetyconstruction.backend.dto.request.camera.CameraUpdateRequest;
import com.safetyconstruction.backend.dto.response.CameraResponse;
import com.safetyconstruction.backend.entity.Camera;
import com.safetyconstruction.backend.entity.Project;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.CameraMapper;
import com.safetyconstruction.backend.repository.CameraRepository;
import com.safetyconstruction.backend.repository.ProjectRepository;
import com.safetyconstruction.backend.specification.CameraSpecification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CameraService {

    CameraRepository cameraRepository;
    CameraMapper cameraMapper;
    ProjectRepository projectRepository;

    /**
     * 🔍 GET CAMERA BY PROJECT
     * - Admin, Manager, Supervisor có thể xem camera trong dự án của mình
     */
    @PreAuthorize("hasAnyAuthority('CAMERA_READ')")
    public Page<CameraResponse> getCamerasByProject(Long projectId, String name, String location, Pageable pageable) {
        log.info("Service: Getting cameras for project {}", projectId);

        Specification<Camera> spec = Specification.allOf(
                CameraSpecification.byProjectId(projectId),
                CameraSpecification.byName(name),
                CameraSpecification.byLocation(location));

        return cameraRepository.findAll(spec, pageable)
                .map(cameraMapper::toCameraResponse);
    }

    /**
     * 🔍 GET ALL CAMERAS
     * - Chỉ Admin có quyền xem toàn bộ camera
     */
    @PreAuthorize("hasAuthority('CAMERA_READ_ALL')")
    public Page<CameraResponse> getAllCameras(String name, String location, Pageable pageable) {
        log.info("Service: Getting all cameras globally");

        Specification<Camera> spec = Specification.allOf(
                CameraSpecification.byName(name),
                CameraSpecification.byLocation(location));

        return cameraRepository.findAll(spec, pageable)
                .map(cameraMapper::toCameraResponse);
    }

    /**
     * 🔍 GET CAMERA BY ID
     * - Admin, Manager, Supervisor có thể xem camera thuộc dự án của mình
     * - Admin có thể xem tất cả
     */
    @PostAuthorize("hasAnyAuthority('CAMERA_READ')")
    public CameraResponse getCameraById(Long id) {
        log.info("Service: Getting camera id {}", id);

        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMERA_NOT_FOUND));

        return cameraMapper.toCameraResponse(camera);
    }

    /**
     * 🧱 CREATE CAMERA
     * - Chỉ Admin, Manager mới có quyền tạo
     */
    @PreAuthorize("hasAuthority('CAMERA_CREATE')")
    public CameraResponse createCameraWithProject(Long projectId, CameraCreationRequest request) {
        log.info("Service: Creating camera for project {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Camera camera = cameraMapper.toCamera(request);
        camera.setProject(project);

        Camera savedCamera = cameraRepository.save(camera);
        return cameraMapper.toCameraResponse(savedCamera);
    }

    /**
     * ✏️ UPDATE CAMERA
     * - Chỉ Admin, Manager mới có quyền cập nhật
     */
    @PreAuthorize("hasAuthority('CAMERA_UPDATE')")
    public CameraResponse updateCamera(Long cameraId, CameraUpdateRequest request) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMERA_NOT_FOUND));

        cameraMapper.updateCamera(camera, request);
        return cameraMapper.toCameraResponse(cameraRepository.save(camera));
    }

    /**
     * ❌ DELETE CAMERA
     * - Chỉ Admin, Manager mới có quyền xóa
     */
    @PreAuthorize("hasAuthority('CAMERA_DELETE')")
    public void deleteCamera(Long id) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMERA_NOT_FOUND));

        cameraRepository.delete(camera);
        log.info("Deleted camera id {}", id);
    }
}
