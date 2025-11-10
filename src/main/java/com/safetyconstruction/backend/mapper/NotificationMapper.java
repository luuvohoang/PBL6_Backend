package com.safetyconstruction.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.safetyconstruction.backend.dto.response.NotificationResponse;
import com.safetyconstruction.backend.entity.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    // Đây là ví dụ đơn giản, bạn có thể cần tùy chỉnh
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "alert.id", target = "alertId")
    NotificationResponse toNotificationResponse(Notification notification);
}
