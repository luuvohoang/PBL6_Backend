package com.safetyconstruction.backend.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import com.safetyconstruction.backend.enums.AlertSeverity;
import com.safetyconstruction.backend.enums.AlertStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id")
    Camera camera;

    @Column(length = 100, nullable = false)
    String type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    AlertSeverity severity;

    @Column(nullable = false)
    Float confidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    AlertStatus alertStatus;

    @Column(name = "happened_at", nullable = false)
    Instant happenedAt;

    @Column(name = "image_key", length = 1024)
    String imageKey;

    @Column(name = "clip_key", length = 1024)
    String clipKey;

    // store JSON metadata as text (DB portability). Convert to JSON at API layer if needed.
    @Lob
    @Column(name = "metadata", columnDefinition = "TEXT")
    String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    User reviewer;

    @Lob
    @Column(name = "review_note", columnDefinition = "TEXT")
    String reviewNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
