package com.safetyconstruction.backend.dto.request.camera;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraUpdateRequest {

    //    @NotNull(message = "ID_REQUIRED")
    //    Long id;

    @Size(min = 3, max = 100, message = "NAME_SIZE")
    String name;

    //    @Pattern(
    //            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
    //            message = "IP_INVALID"
    //    )
    String ipAddress;

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
