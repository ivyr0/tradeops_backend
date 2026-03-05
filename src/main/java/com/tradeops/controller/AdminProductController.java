package com.tradeops.controller;

import com.tradeops.model.request.ProductRequest;
import com.tradeops.model.response.ProductResponse;
import com.tradeops.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    // FR-013
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    // FR-013
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> changeProductStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(productService.changeProductStatus(id, isActive));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }
}