package com.safetyconstruction.backend.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.camera.CameraCreationRequest;
import com.safetyconstruction.backend.dto.request.camera.CameraUpdateRequest;
import com.safetyconstruction.backend.dto.response.CameraResponse;
import com.safetyconstruction.backend.service.CameraService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/projects/{projectId}/cameras")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CameraController {
    CameraService cameraService;

    //    @GetMapping
    //    ApiResponse<List<CameraResponse>> getAllCameras() {
    //        return ApiResponse.<List<CameraResponse>>builder()
    //                .result(cameraService.getAllCameras())
    //                .build();
    //    }
    //
    @GetMapping
    ApiResponse<Page<CameraResponse>> getAllCameras(
            @PathVariable Long projectId, // <-- THÊM THAM SỐ NÀY
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "location", required = false) String location,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        // Gọi đúng phương thức service mới và truyền projectId
        Page<CameraResponse> page = cameraService.getCamerasByProject(projectId, name, location, pageable);
        return ApiResponse.<Page<CameraResponse>>builder().result(page).build();
    }

    @GetMapping("/{cameraId}")
    ApiResponse<CameraResponse> getCameraById(@PathVariable("cameraId") Long cameraId) {
        return ApiResponse.<CameraResponse>builder()
                .result(cameraService.getCameraById(cameraId))
                .build();
    }

    //    @PostMapping
    //    ApiResponse<CameraResponse> createCamera(@RequestBody @Valid CameraCreationRequest request) {
    //        log.info("Controller Creating camera");
    //
    //        return ApiResponse.<CameraResponse>builder()
    //                .result(cameraService.createCamera(request))
    //                .build();
    //    }

    @PostMapping
    public ApiResponse<CameraResponse> createCameraForProject(
            @PathVariable Long projectId, // <-- Lấy projectId từ URL
            @Valid @RequestBody CameraCreationRequest request) {
        log.info("Controller: Adding camera to project {}", projectId);

        // Gọi service và truyền projectId vào
        CameraResponse newCamera = cameraService.createCameraWithProject(projectId, request);

        return ApiResponse.<CameraResponse>builder()
                .message("Camera added to project successfully")
                .result(newCamera)
                .build();
    }

    @PutMapping("/{cameraId}")
    ApiResponse<CameraResponse> updateUser(@PathVariable Long cameraId, @RequestBody CameraUpdateRequest request) {
        return ApiResponse.<CameraResponse>builder()
                .result(cameraService.updateCamera(cameraId, request))
                .build();
    }

    @DeleteMapping("/{cameraId}")
    ApiResponse<String> deleteUser(@PathVariable Long cameraId) {
        cameraService.deleteCamera(cameraId);
        return ApiResponse.<String>builder().result("Camera has been deleted").build();
    }
}
