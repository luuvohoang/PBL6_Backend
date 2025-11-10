// File: src/main/java/com/safetyconstruction/backend/dto/response/StatsResponse.java
package com.safetyconstruction.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    // Tên của nhóm (ví dụ: "NO_HELMET", "NO_VEST")
    private String group;

    // Mảng đếm (7 ngày cho Weekday, 12 tháng cho Monthly)
    private long[] counts;
}
