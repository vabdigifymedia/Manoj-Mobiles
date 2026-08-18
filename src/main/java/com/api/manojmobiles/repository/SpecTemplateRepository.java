package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.SpecTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecTemplateRepository extends JpaRepository<SpecTemplate, UUID> {
    Optional<SpecTemplate> findByCategoryId(UUID categoryId);
}
