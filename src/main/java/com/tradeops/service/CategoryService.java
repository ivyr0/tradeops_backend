package com.tradeops.service;

import com.tradeops.model.request.CategoryRequest;
import com.tradeops.model.response.CategoryResponse;
import com.tradeops.model.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    List<CategoryResponse> getCategoriesByTraderId(Long traderId);
    List<CategoryResponse> getCategoriesByTraderParentIdsAndQuery(Long traderId, Long parentId, String query);

    Page<CategoryResponse> getAllCategories(Pageable pageable);
}
