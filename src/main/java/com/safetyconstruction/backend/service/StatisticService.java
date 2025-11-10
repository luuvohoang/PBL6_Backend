package com.safetyconstruction.backend.service;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyconstruction.backend.dto.request.alert.AlertSearchRequest;
import com.safetyconstruction.backend.dto.response.StatisticResponse;
import com.safetyconstruction.backend.entity.Alert;
import com.safetyconstruction.backend.specification.AlertSpecification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticService {

    EntityManager entityManager;

    /**
     * Lấy số lượng Alert, NHÓM THEO LOẠI VI PHẠM
     * - ADMIN: Toàn quyền
     * - MANAGER: Chỉ xem thống kê projects được phân công
     * - SUPERVISOR: Chỉ xem thống kê cơ bản
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('STATISTIC_READ_ALL', 'STATISTIC_READ_ALERT_TYPE', 'STATISTIC_READ')")
    public List<StatisticResponse> getStatsByType(AlertSearchRequest searchRequest) {
        log.info("Getting statistics by alert type");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StatisticResponse> query = cb.createQuery(StatisticResponse.class);
        Root<Alert> root = query.from(Alert.class);

        // 1. ÁP DỤNG BỘ LỌC (WHERE clause)
        Predicate predicate = buildPredicate(searchRequest, root, cb);
        query.where(predicate);

        // 2. ÁP DỤNG NHÓM (GROUP BY type)
        Expression<String> groupField = root.get("type");
        query.groupBy(groupField);

        // 3. TÍNH TOÁN (SELECT type, COUNT(*))
        query.select(cb.construct(StatisticResponse.class, groupField, cb.count(root)));

        TypedQuery<StatisticResponse> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    /**
     * Lấy số lượng Alert, NHÓM THEO NGÀY TRONG TUẦN
     * - ADMIN: Toàn quyền  
     * - MANAGER: Chỉ xem thống kê projects được phân công
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('STATISTIC_READ_ALL', 'STATISTIC_READ_ALERT_TREND', 'STATISTIC_READ')")
    public List<StatisticResponse> getStatsByWeekday(AlertSearchRequest searchRequest) {
        log.info("Getting statistics by weekday");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StatisticResponse> query = cb.createQuery(StatisticResponse.class);
        Root<Alert> root = query.from(Alert.class);

        // 1. ÁP DỤNG BỘ LỌC (WHERE clause)
        Predicate predicate = buildPredicate(searchRequest, root, cb);
        query.where(predicate);

        // 2. ÁP DỤNG NHÓM (GROUP BY DAY_OF_WEEK(happenedAt))
        Expression<Integer> groupField = cb.function("DAYOFWEEK", Integer.class, root.get("happenedAt"));
        query.groupBy(groupField);

        // 3. TÍNH TOÁN (SELECT DAY_OF_WEEK, COUNT(*))
        query.select(cb.construct(StatisticResponse.class, groupField, cb.count(root)));

        TypedQuery<StatisticResponse> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    /**
     * THÊM MỚI: Thống kê theo Project (Manager cần cái này)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('STATISTIC_READ_ALL', 'STATISTIC_READ_PROJECT', 'STATISTIC_READ')")
    public List<StatisticResponse> getStatsByProject(AlertSearchRequest searchRequest) {
        log.info("Getting statistics by project");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StatisticResponse> query = cb.createQuery(StatisticResponse.class);
        Root<Alert> root = query.from(Alert.class);

        Predicate predicate = buildPredicate(searchRequest, root, cb);
        query.where(predicate);

        // Nhóm theo project
        Expression<Long> groupField = root.get("project").get("id");
        query.groupBy(groupField);

        query.select(cb.construct(StatisticResponse.class, groupField, cb.count(root)));

        TypedQuery<StatisticResponse> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    /**
     * Hàm private để tái sử dụng logic lọc
     */
    private Predicate buildPredicate(AlertSearchRequest searchRequest, Root<Alert> root, CriteriaBuilder cb) {
        Specification<Alert> spec = Specification.allOf(
                AlertSpecification.withProjectId(searchRequest.getProjectId()),
                AlertSpecification.withCameraId(searchRequest.getCameraId()),
                AlertSpecification.withType(searchRequest.getType()),
                AlertSpecification.withSeverity(searchRequest.getSeverity()),
                AlertSpecification.withStatus(searchRequest.getAlertStatus()),
                AlertSpecification.withConfidenceRange(
                        searchRequest.getMinConfidence(), searchRequest.getMaxConfidence()),
                AlertSpecification.withHappenedTimeRange(
                        searchRequest.getHappenedAfter(), searchRequest.getHappenedBefore()));

        return spec.toPredicate(root, cb.createQuery(), cb);
    }
}