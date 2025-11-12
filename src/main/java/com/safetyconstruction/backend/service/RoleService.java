package com.safetyconstruction.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.request.role.RoleCreationRequest;
import com.safetyconstruction.backend.dto.request.role.RoleUpdateRequest;
import com.safetyconstruction.backend.dto.response.RoleResponse;
import com.safetyconstruction.backend.entity.Permission;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.RoleMapper;
import com.safetyconstruction.backend.repository.PermissionRepository;
import com.safetyconstruction.backend.repository.RoleRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    @PreAuthorize("hasRole('ADMIN')") // (Hoặc hasAuthority('ROLE_UPDATE'))
    @Transactional
    public RoleResponse update(String roleName, RoleUpdateRequest request) {
        log.info("Service: Updating role {}", roleName);

        // 1. Tìm Role
        Role role = roleRepository.findById(roleName).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // 2. Cập nhật các trường đơn giản (description)
        roleMapper.updateRole(role, request);

        // 3. Cập nhật Permissions (logic giống 'create')
        if (request.getPermissions() != null) {
            Set<Permission> permissions = request.getPermissions().stream()
                    .map(name -> permissionRepository
                            .findById(name)
                            .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        // 4. Lưu và trả về
        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse create(RoleCreationRequest request) {
        log.info("Service: Creating role {}", request.getName());

        // THÊM: Kiểm tra lỗi Role đã tồn tại (Giống UserService)
        if (roleRepository.existsById(request.getName())) {
            throw new AppException(ErrorCode.ROLE_EXISTED); // Cần tạo ErrorCode này
        }

        var role = roleMapper.toRole(request);

        var permissions = permissionRepository.findAllById(request.getPermissions());

        // KIỂM TRA: Đảm bảo tất cả permission đều hợp lệ
        if (permissions.size() != request.getPermissions().size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND); // Cần tạo ErrorCode này
        }

        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    @PreAuthorize("hasRole('ADMIN')") // THÊM: Bảo mật
    public List<RoleResponse> getAll() {
        log.info("Service: Getting all roles");
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')") // THÊM: Bảo mật
    public void delete(String roleName) {
        log.info("Service: Deleting role {}", roleName);

        // THÊM: Kiểm tra lỗi Role không tồn tại
        if (!roleRepository.existsById(roleName)) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND); // Cần tạo ErrorCode này
        }

        roleRepository.deleteById(roleName);
    }
}
