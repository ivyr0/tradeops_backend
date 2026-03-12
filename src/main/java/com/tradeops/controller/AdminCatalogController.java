package com.tradeops.controller;

import com.tradeops.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final ImageUploadService imageUploadService;

    // FR-013: Upload Product Image
    @PostMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_CATALOG_MANAGER')")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        String imageUrl = imageUploadService.uploadProductImage(id, file);

        return ResponseEntity.ok(Map.of(
                "message", "Image uploaded successfully", 
                "imageUrl", imageUrl
        ));
    }
}
