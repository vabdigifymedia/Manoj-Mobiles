package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.reel.InstagramReelRequestDTO;
import com.api.manojmobiles.dto.reel.InstagramReelResponseDTO;
import com.api.manojmobiles.entity.InstagramReel;
import com.api.manojmobiles.exception.ResourceNotFoundException;
import com.api.manojmobiles.repository.InstagramReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstagramReelService {

    private final InstagramReelRepository reelRepository;

    public List<InstagramReelResponseDTO> getAllActiveReels() {
        return reelRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<InstagramReelResponseDTO> getAllReelsForAdmin() {
        return reelRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public InstagramReelResponseDTO createReel(InstagramReelRequestDTO request) {
        String reelId = extractReelId(request.getReelId());
        
        InstagramReel reel = InstagramReel.builder()
                .reelId(reelId)
                .url(request.getReelId()) // Store the original input as URL just in case
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
                
        return mapToDTO(reelRepository.save(reel));
    }

    @Transactional
    public InstagramReelResponseDTO updateReel(UUID id, InstagramReelRequestDTO request) {
        InstagramReel reel = reelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instagram Reel not found"));

        if (request.getReelId() != null && !request.getReelId().trim().isEmpty()) {
            reel.setReelId(extractReelId(request.getReelId()));
            reel.setUrl(request.getReelId());
        }
        
        if (request.getIsActive() != null) {
            reel.setIsActive(request.getIsActive());
        }
        
        if (request.getDisplayOrder() != null) {
            reel.setDisplayOrder(request.getDisplayOrder());
        }

        return mapToDTO(reelRepository.save(reel));
    }

    @Transactional
    public void deleteReel(UUID id) {
        InstagramReel reel = reelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instagram Reel not found"));
        reelRepository.delete(reel);
    }

    private InstagramReelResponseDTO mapToDTO(InstagramReel reel) {
        return InstagramReelResponseDTO.builder()
                .id(reel.getId())
                .reelId(reel.getReelId())
                .url(reel.getUrl())
                .isActive(reel.getIsActive())
                .displayOrder(reel.getDisplayOrder())
                .createdAt(reel.getCreatedAt())
                .build();
    }

    private String extractReelId(String input) {
        if (input == null) return "";
        input = input.trim();
        // If it looks like a full URL, extract the ID
        if (input.contains("instagram.com")) {
            // e.g. https://www.instagram.com/reel/C899-7Ryz_F/?igsh=...
            Pattern pattern = Pattern.compile("/(?:reel|p)/([^/?]+)");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        // If no match or not a URL, assume it's just the ID
        return input;
    }
}
