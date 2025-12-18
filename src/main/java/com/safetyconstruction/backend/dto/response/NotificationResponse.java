package com.safetyconstruction.backend.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse implements Serializable {

    Long id;

    String userId;

    Long alertId;

    String title;

    String body;

    boolean is_read;

    LocalDateTime createdAt;
}
