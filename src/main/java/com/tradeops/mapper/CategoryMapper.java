package com.tradeops.mapper;

import com.tradeops.model.entity.Category;
import com.tradeops.model.request.CategoryRequest;
import com.tradeops.model.response.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(source = "parent.id", target = "parentId")
    CategoryResponse toCategoryResponse(Category c);
    Category toCategory(CategoryRequest cr);
    Category toCategory(CategoryResponse cr);
    List<CategoryResponse> toCategoryResponseList(List<Category> categories);
}
