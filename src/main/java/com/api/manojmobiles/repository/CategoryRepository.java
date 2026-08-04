package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByParentIsNull();
    List<Category> findByParentId(UUID parentId);
    Optional<Category> findBySlug(String slug);
}
