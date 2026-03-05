package com.tradeops.service;

import com.tradeops.model.request.ProductRequest;
import com.tradeops.model.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    // FR-013: Создание товара (для AdminController)
    ProductResponse createProduct(ProductRequest productRequest);

    // FR-013: Активация/Деактивация товара (вместо физического Delete) (для
    // AdminController)
    ProductResponse changeProductStatus(Long productId, boolean isActive);

    // FR-015: Поиск и фильтрация с пагинацией (для PublicController)
    // Page и Pageable - это встроенные классы Spring Data для постраничной выдачи
    Page<ProductResponse> getProducts(Long traderId, Long categoryId, String query, Pageable pageable);

    // FR-016: Получение одного товара для страницы товара (для PublicController)
    ProductResponse getProductByIdAndTraderId(Long productId, Long traderId);

    // Get all products (для AdminController)
    Page<ProductResponse> getAllProducts(Pageable pageable);

}