package com.safetyconstruction.backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.response.CameraResponse;
import com.safetyconstruction.backend.service.CameraService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GlobalCameraController {

    CameraService cameraService; // Dùng chung CameraService

    @GetMapping
    public ApiResponse<Page<CameraResponse>> getAllCameras(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "location", required = false) String location,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Controller: Getting ALL cameras globally");

        // Gọi phương thức service toàn cục
        Page<CameraResponse> page = cameraService.getAllCameras(name, location, pageable);
        return ApiResponse.<Page<CameraResponse>>builder().result(page).build();
    }

    @GetMapping("/internal/active-list")
    public ApiResponse<List<CameraResponse>> getActiveCamerasForAI() {
        // Gọi hàm mới vừa viết ở Bước 1
        // Lưu ý: Hàm này trả về List, không phải Page -> Hết lỗi
        List<CameraResponse> cameras = cameraService.getAllActiveCameras();

        return ApiResponse.<List<CameraResponse>>builder()
                .code(1000)
                .result(cameras)
                .build();
    }
}
