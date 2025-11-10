package com.safetyconstruction.backend.controller;

import java.util.List;

import com.safetyconstruction.backend.dto.request.role.RoleUpdateRequest;
import org.springframework.web.bind.annotation.*;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.role.RoleCreationRequest;
import com.safetyconstruction.backend.dto.response.RoleResponse;
import com.safetyconstruction.backend.service.RoleService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleController {
    RoleService roleService;

    @PostMapping
    ApiResponse<RoleResponse> create(@RequestBody RoleCreationRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message("Role created successfully")
                .result(roleService.create(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAll())
                .build();
    }

    @PutMapping("/{roleName}")
    ApiResponse<RoleResponse> update(
            @PathVariable String roleName,
            @RequestBody RoleUpdateRequest request
    ) {
        return ApiResponse.<RoleResponse>builder()
                .message("Role updated successfully")
                .result(roleService.update(roleName, request))
                .build();
    }

    @DeleteMapping("/{role}")
    // SỬA: Trả về ApiResponse<String> cho đồng nhất
    ApiResponse<String> delete(@PathVariable String role) {
        roleService.delete(role);
        // SỬA: Trả về message giống UserController
        return ApiResponse.<String>builder().message("Role has been deleted").build();
    }
}
