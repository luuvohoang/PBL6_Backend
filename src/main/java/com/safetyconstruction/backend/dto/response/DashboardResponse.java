// File: src/main/java/com/safetyconstruction/backend/dto/response/DashboardResponse.java
package com.safetyconstruction.backend.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {
    private DashboardSummaryResponse summary;
    private List<StatsResponse> weekdayStats; // (Thay thế detections1, 2, 3)
    private List<StatsResponse> monthlyStats;
    private Page<AlertResponse> recentAlerts; // (Top 5 cảnh báo mới nhất)
}
