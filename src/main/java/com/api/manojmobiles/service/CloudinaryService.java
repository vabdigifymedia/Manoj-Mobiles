package com.api.manojmobiles.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String folderName) throws IOException {
        log.info("Uploading image to Cloudinary: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        java.util.Map<String, Object> options = new java.util.HashMap<>();
        options.put("resource_type", "auto");

        if (folderName != null && !folderName.isBlank()) {
            options.put("folder", "ManojMobiles/" + folderName);
        } else {
            options.put("folder", "ManojMobiles/general");
        }

        int maxRetries = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
                log.info("Successfully uploaded image to Cloudinary on attempt {}", attempt);
                return uploadResult.get("secure_url").toString();
            } catch (Exception e) {
                lastException = e;
                log.warn("Cloudinary upload attempt {} of {} failed: {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.error("All {} Cloudinary upload attempts failed for file: {}", maxRetries, file.getOriginalFilename(), lastException);
        throw new IOException("Cloudinary upload failed after retries: " + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }
}
