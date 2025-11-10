package com.safetyconstruction.backend.dto.request.camera;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraCreationRequest {
    Long id;

    @NotBlank(message = "NAME_EMPTY")
    @Size(min = 3, max = 100, message = "NAME_SIZE")
    String name;

    @NotBlank(message = "IP_EMPTY")
    //    @Pattern(
    //            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
    //            message = "IP_INVALID"
    //    )
    String ipAddress;

    @NotBlank(message = "RTSP_EMPTY")
    //    @Pattern(
    //            regexp = "^(rtsp|rtsps)://.+",
    //            message = "RTSP_INVALID"
    //    )
    String rtspUrl;

    @Size(max = 255, message = "LOCATION_SIZE")
    String location;

    String description;

    @Size(max = 50, message = "MODEL_SIZE")
    String model;
}
