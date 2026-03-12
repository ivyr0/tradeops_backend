package com.tradeops.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "package_artifacts")
public class PackageArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "trader_id", nullable = false)
    @ToString.Include
    private Long traderId;

    @Column(name = "artifact_file_path", nullable = false)
    private String artifactFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_status", nullable = false)
    private BuildStatus buildStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum BuildStatus {
        PENDING, SUCCESS, FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (buildStatus == null) {
            buildStatus = BuildStatus.PENDING;
        }
    }
}
