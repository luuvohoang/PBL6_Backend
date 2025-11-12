package com.safetyconstruction.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.safetyconstruction.backend.dto.request.project.ProjectCreationRequest;
import com.safetyconstruction.backend.dto.request.project.ProjectUpdateRequest;
import com.safetyconstruction.backend.dto.response.ProjectResponse;
import com.safetyconstruction.backend.entity.Project;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class, CameraMapper.class})
public interface ProjectMapper {

    ProjectResponse toProjectResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cameras", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Project toProject(ProjectCreationRequest request);

    // --- THÊM PHƯƠNG THỨC NÀY ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "cameras", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateProject(@MappingTarget Project project, ProjectUpdateRequest request);
}
