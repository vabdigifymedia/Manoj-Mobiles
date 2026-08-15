package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.StoreSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreSettingRepository extends JpaRepository<StoreSetting, String> {
}
