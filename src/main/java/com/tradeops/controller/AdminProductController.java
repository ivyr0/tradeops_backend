package com.tradeops.controller;

import com.tradeops.model.request.ChangeProductStatusRequest;
import com.tradeops.model.request.CreateProductRequest;
import com.tradeops.model.request.DeleteProductRequest;
import com.tradeops.model.response.ProductResponse;
import com.tradeops.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    // FR-013
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    // FR-013
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> changeProductStatus(
            @PathVariable Long id,
            @RequestBody @Valid ChangeProductStatusRequest request) {
        return ResponseEntity.ok(productService.changeProductStatus(id, request.isActive()));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }
}