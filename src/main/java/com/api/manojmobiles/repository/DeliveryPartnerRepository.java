package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, UUID> {
}
