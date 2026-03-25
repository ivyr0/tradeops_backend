package com.tradeops.controller;

import com.tradeops.model.entity.Trader;
import com.tradeops.model.request.ChangeTraderStatusRequest;
import com.tradeops.model.request.TraderRequests.CreateTraderRequest;
import com.tradeops.model.request.TraderRequests.UpdateTraderRequest;
import com.tradeops.model.response.TraderResponse;
import com.tradeops.service.PackageBuildService;
import com.tradeops.service.impl.TraderManagementServiceImpl;
import org.springframework.http.HttpHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/superadmin/traders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminTraderController {

    private final TraderManagementServiceImpl traderManagementService;
    private final PackageBuildService packageBuildService;

    @PostMapping
    public ResponseEntity<TraderResponse> createTrader(@Valid @RequestBody CreateTraderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(traderManagementService.createTrader(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TraderResponse> changeStatus(@PathVariable Long id,
            @RequestBody @Valid ChangeTraderStatusRequest request) {
        return ResponseEntity.ok(traderManagementService.changeStatus(id, request.status()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TraderResponse> updateTrader(@PathVariable Long id,
            @Valid @RequestBody UpdateTraderRequest request) {
        return ResponseEntity.ok(traderManagementService.updateTrader(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<TraderResponse>> getAllTraders(@PageableDefault(size = 15) Pageable pageable) {
        return ResponseEntity.ok(traderManagementService.getAllTraders(pageable));
    }

    @PostMapping("/{id}/package/build")
    public ResponseEntity<byte[]> downloadTraderPackage(@PathVariable Long id) throws java.io.IOException {
        byte[] zipArchive = packageBuildService.generateTraderPackage(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trader-" + id + "-package.zip\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .body(zipArchive);
    }
}
