package com.tradeops.controller;

import com.tradeops.model.entity.Trader;
import com.tradeops.model.request.TraderRequests.CreateTraderRequest;
import com.tradeops.model.request.TraderRequests.UpdateTraderRequest;
import com.tradeops.service.impl.TraderManagementServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/superadmin/traders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminTraderController {

    private final TraderManagementServiceImpl traderManagementService;

    @PostMapping
    public ResponseEntity<Trader> createTrader(@Valid @RequestBody CreateTraderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(traderManagementService.createTrader(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Trader> changeStatus(@PathVariable Long id, @RequestParam Trader.TraderStatus status) {
        return ResponseEntity.ok(traderManagementService.changeStatus(id, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trader> updateTrader(@PathVariable Long id, @Valid @RequestBody UpdateTraderRequest request) {
        return ResponseEntity.ok(traderManagementService.updateTrader(id, request));
    }

    @GetMapping
    public ResponseEntity<java.util.List<Trader>> getAllTraders() {
        return ResponseEntity.ok(traderManagementService.getAllTraders());
    }
}
