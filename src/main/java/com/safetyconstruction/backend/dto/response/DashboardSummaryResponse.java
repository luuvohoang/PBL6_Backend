// File: src/main/java/com/safetyconstruction/backend/dto/response/DashboardSummaryResponse.java
package com.safetyconstruction.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponse {
    private long totalAlerts; // Tổng số cảnh báo
    private long unresolvedAlerts; // Số cảnh báo chưa xử lý (NEW)
    private long highSeverityAlerts; // Số cảnh báo mức độ HIGH/CRITICAL
}
