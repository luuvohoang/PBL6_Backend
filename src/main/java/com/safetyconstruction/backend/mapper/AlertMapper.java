package com.safetyconstruction.backend.mapper;

import java.util.List;

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

    // Đây là hàm chính để chuyển từ Entity sang DTO trả về cho React
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "camera.id", target = "cameraId")
    @Mapping(source = "camera.name", target = "cameraName")
    @Mapping(source = "camera.location", target = "location")
    AlertResponse toAlertResponse(Alert alert);

    // Nếu bạn có danh sách, MapStruct sẽ tự dùng quy tắc ở hàm trên cho hàm này
    List<AlertResponse> toAlertResponseList(List<Alert> alerts);

    // 2. Mapper cho 'create'
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", source = "project")
    @Mapping(target = "camera", source = "camera")
    @Mapping(target = "alertStatus", constant = "NEW")
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "reviewNote", ignore = true)
    Alert toAlert(AlertCreationRequest request, Project project, Camera camera);

    // 3. Mapper cho 'review'
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "camera", ignore = true)
    @Mapping(target = "reviewer", source = "reviewer")
    void updateAlertFromReview(@MappingTarget Alert alert, AlertReviewRequest request, User reviewer);
}
