package com.tradeops.service;

import com.tradeops.model.request.CategoryRequest;
import com.tradeops.model.request.EditCategoryRequest;
import com.tradeops.model.response.CategoryResponse;
import com.tradeops.model.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    Page<CategoryResponse> getCategoriesByTraderId(Long traderId, Pageable pageable);
    Page<CategoryResponse> getCategoriesByTraderParentIdsAndQuery(Long traderId, Long parentId, String query, Pageable pageable);

    Page<CategoryResponse> getAllCategories(Pageable pageable);

    Void deleteCategory(Long id);

    CategoryResponse editCategory(EditCategoryRequest request, Long id);
}
