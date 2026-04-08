package com.tradeops.controller;

import com.tradeops.model.request.CourierCreateRequest;
import com.tradeops.model.request.CourierUpdateRequest;
import com.tradeops.model.request.ToggleCourierStatusRequest;
import com.tradeops.model.response.CourierUserResponse;
import com.tradeops.service.CourierAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/couriers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_MODERATOR', 'ROLE_DISPATCHER')")
public class AdminCourierController {

    private final CourierAdminService courierAdminService;

    @PostMapping
    public ResponseEntity<CourierUserResponse> createCourier(@Valid @RequestBody CourierCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courierAdminService.createCourier(request));
    }

    @GetMapping
    public ResponseEntity<Page<CourierUserResponse>> getAllCouriers(Pageable pageable) {
        return ResponseEntity.ok(courierAdminService.getAllCouriers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourierUserResponse> getCourierById(@PathVariable Long id) {
        return ResponseEntity.ok(courierAdminService.getCourierById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourierUserResponse> updateCourier(
            @PathVariable Long id, 
            @Valid @RequestBody CourierUpdateRequest request) {
        return ResponseEntity.ok(courierAdminService.updateCourier(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleCourierStatus(
            @PathVariable Long id, 
            @RequestBody @Valid ToggleCourierStatusRequest request) {
        courierAdminService.toggleCourierStatus(id, request.status());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourier(@PathVariable Long id) {
        return ResponseEntity.ok(courierAdminService.deleteCourier(id));
    }

}
