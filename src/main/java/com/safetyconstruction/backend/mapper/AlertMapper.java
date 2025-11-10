package com.safetyconstruction.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.safetyconstruction.backend.dto.request.alert.AlertCreationRequest;
import com.safetyconstruction.backend.dto.request.alert.AlertReviewRequest;
import com.safetyconstruction.backend.dto.response.AlertResponse;
import com.safetyconstruction.backend.entity.Alert;
import com.safetyconstruction.backend.entity.Camera;
import com.safetyconstruction.backend.entity.Project;
import com.safetyconstruction.backend.entity.User;

@Mapper(componentModel = "spring")
public interface AlertMapper {
    @Mapping(source = "project.id", target = "projectId") // <-- SỬA LỖI 1
    @Mapping(source = "camera.id", target = "cameraId") // <-- SỬA LỖI 2
    @Mapping(source = "alertStatus", target = "alertStatus") // (Đã có trong DTO của bạn)
    @Mapping(source = "reviewer.id", target = "reviewerId")
    AlertResponse toAlert(Alert alert);

    // 2. Mapper cho 'create' (thay thế .builder())
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", source = "project")
    @Mapping(target = "camera", source = "camera")
    @Mapping(target = "alertStatus", constant = "NEW") // Tự động gán
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "reviewNote", ignore = true)
    Alert toAlert(AlertCreationRequest request, Project project, Camera camera);

    // 3. Mapper cho 'review' (thay thế .set...)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "camera", ignore = true)
    @Mapping(target = "reviewer", source = "reviewer")
    void updateAlertFromReview(@MappingTarget Alert alert, AlertReviewRequest request, User reviewer);

    AlertResponse toAlertResponse(Alert alert);
}
