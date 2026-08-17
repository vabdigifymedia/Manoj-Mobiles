package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.InstagramReel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstagramReelRepository extends JpaRepository<InstagramReel, UUID> {
    List<InstagramReel> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<InstagramReel> findAllByOrderByDisplayOrderAsc();
}
