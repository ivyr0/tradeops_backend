package com.tradeops.repo;

import com.tradeops.model.entity.Category;
import com.tradeops.model.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    Page<Category> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"subcategories"})
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    Page<Category> findRootCategories(Pageable pageable);

}

