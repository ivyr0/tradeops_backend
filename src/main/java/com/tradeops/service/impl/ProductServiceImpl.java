package com.tradeops.service.impl;

import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.exceptions.TenantAccessDeniedException;
import com.tradeops.model.entity.Category;
import com.tradeops.model.entity.InventoryItem;
import com.tradeops.model.entity.Product;
import com.tradeops.model.request.ProductRequest;
import com.tradeops.model.response.ProductResponse;
import com.tradeops.repo.CategoryRepo;
import com.tradeops.repo.ProductRepo;
import com.tradeops.repo.TraderRepo;
import com.tradeops.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final TraderRepo traderRepo;

    // FR-013
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepo.findById(request.categoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category with ID " + request.categoryId() + " not found"));

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setActive(request.isActive());
        product.setImages(request.images());
        product.setCategory(category);

        Product savedProduct = productRepo.save(product);
        return mapToResponse(savedProduct);
    }

    // FR-013
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse changeProductStatus(Long productId, boolean isActive) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));

        product.setActive(isActive);
        return mapToResponse(productRepo.save(product));
    }

    // FR-015
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Long traderId, Long categoryId, String query, Pageable pageable) {
        List<Long> allowedCategoryIds = traderRepo.findCategoryIdsById(traderId);

        if (allowedCategoryIds == null || allowedCategoryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Product> productsPage = productRepo.findStorefrontProducts(
                allowedCategoryIds, categoryId, query, pageable);

        return productsPage.map(this::mapToResponse);
    }

    // FR-016
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByIdAndTraderId(Long productId, Long traderId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<Long> allowedCategoryIds = traderRepo.findCategoryIdsById(traderId);
        if (allowedCategoryIds == null || !allowedCategoryIds.contains(product.getCategory().getId())) {
            throw new TenantAccessDeniedException("Trader is not allowed to access this product");
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productsPage = productRepo.findAll(pageable);
        return productsPage.map(this::mapToResponse);
    }

    private ProductResponse mapToResponse(Product product) {
        Integer availableQty = 0;

        if (product.getInventoryItems() != null && !product.getInventoryItems().isEmpty()) {
            InventoryItem inventory = product.getInventoryItems().get(0);
            availableQty = inventory.getQtyOnHand() - inventory.getQtyReserved();
        }

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.isActive(),
                product.getImages(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                availableQty);
    }
}