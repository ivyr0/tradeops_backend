package com.tradeops.service.impl;

import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.mapper.CategoryMapper;
import com.tradeops.model.entity.Category;
import com.tradeops.model.request.CategoryRequest;
import com.tradeops.model.response.CategoryResponse;
import com.tradeops.repo.CategoryRepo;
import com.tradeops.repo.TraderRepo;
import com.tradeops.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepo categoryRepo;
    private final TraderRepo tr;
    private final CategoryMapper cm;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(CategoryRequest cr) {
        if (!isCategoryRequestValid(cr)) {
            throw new IllegalArgumentException("Invalid category request");
        }
        Category c = new Category();
        c.setName(cr.name());
        c.setSlug(cr.slug());
        if(cr.parentId() != null){
            Category parent = categoryRepo.findById(cr.parentId()).orElseThrow(()->new ResourceNotFoundException("Parent category not found"));
            c.setParent(parent);
        }
        c.setSortOrder(cr.sortOrder() != null ? cr.sortOrder() : 0);
        categoryRepo.save(c);
        return cm.toCategoryResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByTraderId(Long traderId) {
        if(traderId == null){
            throw new IllegalArgumentException("Trader id cannot be null");
        }

        return cm.toCategoryResponseList(getAllowedCategories(traderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByTraderParentIdsAndQuery(Long traderId, Long parentId, String query) {
        if(traderId == null){
            throw new IllegalArgumentException("Trader id cannot be null");
        }

        List<Category> allowedCategories = getAllowedCategories(traderId);
        List<Category> filteredList = allowedCategories.stream()
                .filter(c -> {
                    boolean matchesParent = (parentId == null) ||
                            (c.getParent() != null && c.getParent().getId().equals(parentId));

                    boolean matchesQuery = (query == null || query.isBlank()) ||
                            c.getName().toLowerCase().contains(query.toLowerCase());

                    return matchesParent && matchesQuery;
                })
                .toList();
        return cm.toCategoryResponseList(filteredList);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        Page<Category> categoryPage = categoryRepo.findRootCategories(pageable);
        return categoryPage.map(cm::toCategoryResponse);
    }


    private boolean isCategoryRequestValid(CategoryRequest cr) {
        return cr.name() != null && cr.slug() != null && cr.sortOrder() != null;
    }

    private List<Category> getAllowedCategories(Long traderId){
        return categoryRepo.findAllById(tr.findCategoryIdsById(traderId));
    }
}
