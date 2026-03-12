package com.tradeops.repo;

import com.tradeops.model.entity.PackageArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageArtifactRepo extends JpaRepository<PackageArtifact, Long> {
    List<PackageArtifact> findByTraderIdOrderByCreatedAtDesc(Long traderId);
}
