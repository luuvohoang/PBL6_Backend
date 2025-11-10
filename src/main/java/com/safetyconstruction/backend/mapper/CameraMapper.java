package com.safetyconstruction.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.safetyconstruction.backend.dto.request.camera.CameraCreationRequest;
import com.safetyconstruction.backend.dto.request.camera.CameraUpdateRequest;
import com.safetyconstruction.backend.dto.response.CameraResponse;
import com.safetyconstruction.backend.entity.Camera;

@Mapper(componentModel = "spring")
public interface CameraMapper {

    Camera toCamera(CameraCreationRequest request);

    void updateCamera(@MappingTarget Camera camera, CameraUpdateRequest request);

    @Mapping(source = "project.id", target = "projectId")
    CameraResponse toCameraResponse(Camera camera);
}
