package com.safetyconstruction.backend.mapper;

import org.mapstruct.Mapper;

import com.safetyconstruction.backend.dto.request.PermissionRequest;
import com.safetyconstruction.backend.dto.response.PermissionResponse;
import com.safetyconstruction.backend.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
