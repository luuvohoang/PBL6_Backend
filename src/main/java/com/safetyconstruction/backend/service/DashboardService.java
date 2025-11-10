// File: src/main/java/com/safetyconstruction/backend/service/DashboardService.java
package com.safetyconstruction.backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.AlertResponse;
import com.safetyconstruction.backend.dto.response.DashboardResponse;
import com.safetyconstruction.backend.dto.response.DashboardSummaryResponse;
import com.safetyconstruction.backend.dto.response.StatsResponse;
import com.safetyconstruction.backend.entity.Alert;
import com.safetyconstruction.backend.repository.AlertRepository;
import com.safetyconstruction.backend.specification.AlertSpecification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService {

    EntityManager entityManager;
    AlertRepository alertRepository;
    AlertService alertService; // Tái sử dụng AlertService

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public DashboardResponse getDashboardData(AlertSearchRequest searchRequest) {

        // 1. Lấy dữ liệu Summary (KPI Cards)
        DashboardSummaryResponse summary = getSummaryStats(searchRequest);

        // 2. Lấy dữ liệu biểu đồ theo Ngày trong tuần
        List<StatsResponse> weekdayStats = getWeekdayStats(searchRequest);

        // 3. Lấy dữ liệu biểu đồ theo Tháng
        List<StatsResponse> monthlyStats = getMonthlyStats(searchRequest);

        // 4. Lấy 5 cảnh báo mới nhất
        Pageable recentAlertsPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "happenedAt"));
        Page<AlertResponse> recentAlerts = alertService.searchAlerts(searchRequest, recentAlertsPageable);

        // 5. Gộp tất cả vào 1 Response
        return DashboardResponse.builder()
                .summary(summary)
                .weekdayStats(weekdayStats)
                .monthlyStats(monthlyStats)
                .recentAlerts(recentAlerts)
                .build();
    }

    // --- CÁC HÀM TÍNH TOÁN (PRIVATE) ---

    // Hàm này lấy các thẻ KPI (Tổng, Mới, Nghiêm trọng)
    private DashboardSummaryResponse getSummaryStats(AlertSearchRequest request) {
        Specification<Alert> baseSpec = buildSpecification(request);

        // Lấy Tổng
        long totalAlerts = alertRepository.count(baseSpec);

        // Lấy Mới (Unresolved)
        Specification<Alert> unresolvedSpec = baseSpec.and(AlertSpecification.withStatus("NEW"));
        long unresolvedAlerts = alertRepository.count(unresolvedSpec);

        // Lấy Nghiêm trọng
        Specification<Alert> criticalSpec = baseSpec.and(Specification.anyOf(
                AlertSpecification.withSeverity("HIGH"), AlertSpecification.withSeverity("CRITICAL")));
        long highSeverityAlerts = alertRepository.count(criticalSpec);

        return DashboardSummaryResponse.builder()
                .totalAlerts(totalAlerts)
                .unresolvedAlerts(unresolvedAlerts)
                .highSeverityAlerts(highSeverityAlerts)
                .build();
    }

    // Hàm này thay thế logic 'count()' và 'partitionByErrorType' (theo ngày)
    private List<StatsResponse> getWeekdayStats(AlertSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Alert> root = query.from(Alert.class);

        // 1. Lọc (WHERE)
        Predicate predicate = buildPredicate(request, root, cb);

        // --- SỬA LỖI Ở ĐÂY ---
        // Chỉ áp dụng mệnh đề WHERE nếu 'predicate' (bộ lọc) không rỗng
        if (predicate != null) {
            query.where(predicate); // <-- Đây là dòng 104
        }

        // 2. Nhóm (GROUP BY)
        Expression<String> typeGroup = root.get("type");
        // Dùng hàm DAYOFWEEK() của MySQL (1=CN, 2=T2, ...)
        Expression<Integer> weekdayGroup = cb.function("DAYOFWEEK", Integer.class, root.get("happenedAt"));
        query.groupBy(typeGroup, weekdayGroup);

        // 3. Đếm (SELECT)
        query.multiselect(
                typeGroup.alias("group"),
                weekdayGroup.alias("day"),
                cb.count(root).alias("count"));

        // Xử lý kết quả thô (raw)
        List<Tuple> results = entityManager.createQuery(query).getResultList();

        // 4. Chuyển đổi (Process)
        // Chuyển List<Tuple> (ví dụ: ["NO_HELMET", 2, 10])
        // thành Map<String, long[7]> (ví dụ: "NO_HELMET" -> [0, 10, 0, 0, 0, 0, 0])
        Map<String, long[]> statsMap = results.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get("group", String.class),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            long[] counts = new long[7]; // 0=CN, 1=T2, ...
                            list.forEach(tuple -> {
                                int dayOfWeek = tuple.get("day", Integer.class); // 1-7
                                long count = tuple.get("count", Long.class);
                                counts[dayOfWeek - 1] = count; // Chuyển 1-7 thành index 0-6
                            });
                            return counts;
                        })));

        // Chuyển Map thành List<StatsResponse>
        return statsMap.entrySet().stream()
                .map(entry -> new StatsResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // Hàm này thay thế logic 'getMonthlyReportData'
    private List<StatsResponse> getMonthlyStats(AlertSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Alert> root = query.from(Alert.class);

        // 1. Lọc (WHERE)
        Predicate predicate = buildPredicate(request, root, cb);

        // --- SỬA LỖI Ở ĐÂY (Giống hệt lần trước) ---
        // Chỉ áp dụng mệnh đề WHERE nếu 'predicate' (bộ lọc) không rỗng
        if (predicate != null) {
            query.where(predicate); // <-- Đây là dòng 155
        }
        // --- HẾT PHẦN SỬA ---

        // 2. Nhóm (GROUP BY)
        Expression<String> typeGroup = root.get("type");
        // Dùng hàm MONTH() của MySQL (1-12)
        Expression<Integer> monthGroup = cb.function("MONTH", Integer.class, root.get("happenedAt"));
        query.groupBy(typeGroup, monthGroup);

        // 3. Đếm (SELECT)
        query.multiselect(
                typeGroup.alias("group"),
                monthGroup.alias("month"),
                cb.count(root).alias("count"));

        // Xử lý kết quả thô
        List<Tuple> results = entityManager.createQuery(query).getResultList();

        // 4. Chuyển đổi (Process)
        Map<String, long[]> statsMap = results.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get("group", String.class),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            long[] counts = new long[12]; // 0=Tháng 1, ...
                            list.forEach(tuple -> {
                                int month = tuple.get("month", Integer.class); // 1-12
                                long count = tuple.get("count", Long.class);
                                counts[month - 1] = count; // Chuyển 1-12 thành index 0-11
                            });
                            return counts;
                        })));

        return statsMap.entrySet().stream()
                .map(entry -> new StatsResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Hàm private để tái sử dụng logic lọc (filtering) của AlertSpecification
     */
    private Predicate buildPredicate(AlertSearchRequest searchRequest, Root<Alert> root, CriteriaBuilder cb) {
        Specification<Alert> spec = Specification.allOf(
                AlertSpecification.withProjectId(searchRequest.getProjectId()),
                AlertSpecification.withCameraId(searchRequest.getCameraId()),
                AlertSpecification.withType(searchRequest.getType()),
                AlertSpecification.withSeverity(searchRequest.getSeverity()),
                AlertSpecification.withStatus(searchRequest.getAlertStatus()),
                AlertSpecification.withHappenedTimeRange(
                        searchRequest.getHappenedAfter(), searchRequest.getHappenedBefore()));
        return spec.toPredicate(root, cb.createQuery(), cb);
    }

    private Specification<Alert> buildSpecification(AlertSearchRequest searchRequest) {
        // Đây là cách chúng ta kết hợp tất cả các Specification bạn đã viết
        // Nó giống hệt logic trong hàm 'buildPredicate'
        return Specification.allOf(
                AlertSpecification.withProjectId(searchRequest.getProjectId()),
                AlertSpecification.withCameraId(searchRequest.getCameraId()),
                AlertSpecification.withType(searchRequest.getType()),
                AlertSpecification.withSeverity(searchRequest.getSeverity()),
                AlertSpecification.withStatus(searchRequest.getAlertStatus()),
                AlertSpecification.withConfidenceRange( // (Tôi cũng thêm bộ lọc này)
                        searchRequest.getMinConfidence(), searchRequest.getMaxConfidence()),
                AlertSpecification.withHappenedTimeRange(
                        searchRequest.getHappenedAfter(), searchRequest.getHappenedBefore()));
    }
}
