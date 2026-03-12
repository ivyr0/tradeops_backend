package com.tradeops.service;

import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.model.entity.Product;
import com.tradeops.repo.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final ProductRepo productRepo;

    // Define the local storage directory mapping matching the WebMvcConfig
    private final Path productImageLocation = Paths.get("uploads/images/products");

    @Transactional
    public String uploadProductImage(Long productId, MultipartFile file) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        try {
            if (!Files.exists(productImageLocation)) {
                Files.createDirectories(productImageLocation);
            }

            // Generate a unique filename to prevent overwrites
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Physical path of the file
            Path targetLocation = productImageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation);

            // Construct the HTTP reachable URL (From WebMvcConfig: /uploads/** maps to uploads/)
            String fileUrl = "/uploads/images/products/" + newFilename;

            // Append URL to the database
            if (product.getImages() == null) {
                product.setImages(new java.util.ArrayList<>());
            }
            product.getImages().add(fileUrl);
            productRepo.save(product);

            log.info("Uploaded product image for Product ID {}: {}", productId, fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to store image file.", e);
            throw new RuntimeException("Could not store the file. Please try again! " + e.getMessage());
        }
    }
}
