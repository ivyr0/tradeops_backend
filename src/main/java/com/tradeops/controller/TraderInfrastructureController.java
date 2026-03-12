package com.tradeops.controller;

import com.tradeops.model.entity.TraderUser;
import com.tradeops.model.request.TraderRequests.CreatePersonnelRequest;
import com.tradeops.model.request.TraderRequests.ThemeConfigRequest;
import com.tradeops.model.response.TraderUserResponse;
import com.tradeops.service.impl.TraderInfrastructureServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trader/{traderId}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_TRADER_ADMIN')")
public class TraderInfrastructureController {

    private final TraderInfrastructureServiceImpl infrastructureService;

    @PostMapping("/personnel")
    public ResponseEntity<TraderUserResponse> addPersonnel(
            @PathVariable Long traderId,
            @Valid @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(infrastructureService.addPersonnel(traderId, request));
    }

    @PatchMapping("/settings/theme")
    public ResponseEntity<Void> updateTheme(
            @PathVariable Long traderId,
            @Valid @RequestBody ThemeConfigRequest request) {
        infrastructureService.updateTheme(traderId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settings/ssl")
    public ResponseEntity<String> uploadSsl(@PathVariable Long traderId) {
        infrastructureService.uploadSslCertificate(traderId);
        return ResponseEntity.ok("SSL Certificate accepted for processing");
    }

    @PostMapping("/build")
    public ResponseEntity<String> triggerBuild(@PathVariable Long traderId) {
        infrastructureService.triggerFrontendBuild(traderId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Build triggered successfully. It will complete in background.");
    }
}
