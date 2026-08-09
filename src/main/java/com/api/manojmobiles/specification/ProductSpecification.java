package com.api.manojmobiles.specification;

import com.api.manojmobiles.dto.product.ProductFilterRequestDTO;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    public static Specification<ProductVariant> getProductsByFilter(ProductFilterRequestDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by product status == ACTIVE
            predicates.add(criteriaBuilder.equal(root.get("product").get("status"), ProductStatus.ACTIVE));

            if (filter.getQuery() != null && StringUtils.hasText(filter.getQuery())) {
                String likeQuery = "%" + filter.getQuery().toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("product").get("name")), likeQuery);
                Predicate variantNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("variantName")), likeQuery);
                Predicate categoryMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("product").get("category").get("name")), likeQuery);
                Predicate brandMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("product").get("brand").get("name")), likeQuery);

                predicates.add(criteriaBuilder.or(nameMatch, variantNameMatch, categoryMatch, brandMatch));
            }

            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("category").get("id"), filter.getCategoryId()));
            }

            if (filter.getBrandId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("brand").get("id"), filter.getBrandId()));
            }

            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sellingPrice"), filter.getMinPrice()));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("sellingPrice"), filter.getMaxPrice()));
            }

            if (filter.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("product").get("avgRating"), filter.getMinRating()));
            }

            if (Boolean.TRUE.equals(filter.getInStockOnly())) {
                predicates.add(criteriaBuilder.greaterThan(root.get("stockQty"), 0));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
