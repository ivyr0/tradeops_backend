package com.tradeops.model.response;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Long parentId,
        List<CategoryResponse> subcategories

) {}
