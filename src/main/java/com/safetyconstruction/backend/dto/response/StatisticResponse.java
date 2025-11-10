// File: src/main/java/com/safetyconstruction/backend/dto/response/StatisticResponse.java
package com.safetyconstruction.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticResponse {

    // Tên của nhóm (ví dụ: "NO_HELMET", "Thứ Hai")
    private Object group;

    // Số lượng
    private long count;
}
