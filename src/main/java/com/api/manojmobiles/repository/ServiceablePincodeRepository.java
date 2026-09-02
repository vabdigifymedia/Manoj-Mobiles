package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ServiceablePincode;
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
public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, UUID> {
    Optional<ServiceablePincode> findByPincode(String pincode);
    List<ServiceablePincode> findByCityId(UUID cityId);
    boolean existsByPincode(String pincode);

    @Query("SELECT p FROM ServiceablePincode p WHERE " +
            "(:cityId IS NULL OR p.city.id = :cityId) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(p.pincode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.city.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.city.state) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ServiceablePincode> searchPincodes(@Param("cityId") UUID cityId, @Param("search") String search, Pageable pageable);
}
