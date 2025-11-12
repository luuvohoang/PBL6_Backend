package com.safetyconstruction.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.safetyconstruction.backend.dto.request.role.RoleCreationRequest;
import com.safetyconstruction.backend.dto.request.role.RoleUpdateRequest;
import com.safetyconstruction.backend.dto.response.RoleResponse;
import com.safetyconstruction.backend.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleCreationRequest request);

    RoleResponse toRoleResponse(Role role);

    @Mapping(target = "name", ignore = true) // Không cập nhật 'name'
    @Mapping(target = "permissions", ignore = true) // Sẽ xử lý thủ công trong Service
    void updateRole(@MappingTarget Role role, RoleUpdateRequest request);
}
