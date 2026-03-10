package com.tradeops.controller;

import com.tradeops.model.entity.DeliveryAssignment;
import com.tradeops.model.response.DeliveryAssignmentResponse;
import com.tradeops.service.CourierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courier/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_COURIER')")
public class CourierController {

    private final CourierService courierService;

    @GetMapping
    public ResponseEntity<List<DeliveryAssignmentResponse>> getMyFeed() {
        return ResponseEntity.ok(courierService.getActiveAssignments());
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<DeliveryAssignmentResponse> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(courierService.acceptAssignment(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<DeliveryAssignmentResponse> completeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(courierService.completeAssignment(id));
    }
}
