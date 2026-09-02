package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.City;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {
    List<City> findByIsActiveTrue();
    Optional<City> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    @Query(value = "SELECT DISTINCT c FROM City c LEFT JOIN c.pincodes p WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "p.pincode LIKE CONCAT('%', :search, '%'))",
            countQuery = "SELECT COUNT(DISTINCT c) FROM City c LEFT JOIN c.pincodes p WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "p.pincode LIKE CONCAT('%', :search, '%'))")
    Page<City> searchCities(@Param("search") String search, Pageable pageable);
}
