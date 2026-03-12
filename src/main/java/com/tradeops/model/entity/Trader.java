package com.tradeops.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Table(name = "traders")
@AllArgsConstructor
@NoArgsConstructor
public class Trader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(name = "legal_name", nullable = false)
    @ToString.Include
    private String legalName;

    @Column(name = "display_name", nullable = false)
    @ToString.Include
    private String displayName;

    @Column(unique = true)
    private String domain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "theme_config_json", columnDefinition = "TEXT")
    private String themeConfigJson;

    @ElementCollection
    @CollectionTable(name = "trader_allowed_categories", joinColumns = @JoinColumn(name = "trader_id"))
    @Column(name = "category_id")
    private List<Long> allowedCategoryIds;

    @OneToMany(mappedBy = "trader", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TraderUser> traderUsers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TraderStatus status;

    @Column(name = "ssl_cert_path")
    private String sslCertPath;

    @Column(name = "ssl_key_db_encrypted")
    private String SslKeyDbEncrypted;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = TraderStatus.PENDING;
        }
    }

    public enum TraderStatus {
        PENDING, ACTIVE, INACTIVE
    }
}