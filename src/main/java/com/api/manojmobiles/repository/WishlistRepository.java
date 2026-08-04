package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByUserId(UUID userId);
    Optional<Wishlist> findByUserIdAndVariantId(UUID userId, UUID variantId);
    void deleteByUserIdAndVariantId(UUID userId, UUID variantId);
}
