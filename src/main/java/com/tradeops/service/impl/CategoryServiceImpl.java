package com.tradeops.service.impl;

import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.mapper.CategoryMapper;
import com.tradeops.model.entity.Category;
import com.tradeops.model.request.CategoryRequest;
import com.tradeops.model.request.EditCategoryRequest;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse editCategory(EditCategoryRequest request, Long id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setName(request.name);

        return cm.toCategoryResponse(category);
    }

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
    public Page<CategoryResponse> getCategoriesByTraderId(Long traderId, Pageable pageable) {
        if(traderId == null){
            throw new IllegalArgumentException("Trader id cannot be null");
        }

        Page<Category> allowedCategories = getAllowedCategories(traderId, pageable);
        return allowedCategories.map(cm::toCategoryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategoriesByTraderParentIdsAndQuery(Long traderId, Long parentId, String query, Pageable pageable) {
        if(traderId == null){
            throw new IllegalArgumentException("Trader id cannot be null");
        }

        List<Long> allowedCategoryIds = tr.findCategoryIdsById(traderId);
        if(allowedCategoryIds == null || allowedCategoryIds.isEmpty()){
            return Page.empty(pageable);
        }

        Page<Category> allowedCategories = categoryRepo.findAllCategoriesFiltered(allowedCategoryIds, parentId, query, pageable);
        return allowedCategories.map(cm::toCategoryResponse);
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

    private Page<Category> getAllowedCategories(Long traderId, Pageable pageable){
        return categoryRepo.findAllByIdIn(tr.findCategoryIdsById(traderId), pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void deleteCategory(Long id) {
        if (!categoryRepo.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepo.deleteById(id);
        return null;
    }
}
