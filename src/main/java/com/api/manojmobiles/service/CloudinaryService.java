package com.api.manojmobiles.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
        log.info("Uploading image to Cloudinary: {}", file.getOriginalFilename());
        
        java.util.Map<String, Object> options = new java.util.HashMap<>();
        options.put("resource_type", "auto");
        
        if (folderName != null && !folderName.isBlank()) {
            options.put("folder", "ManojMobiles/" + folderName);
        } else {
            options.put("folder", "ManojMobiles/uncategorized");
        }

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        
        return uploadResult.get("secure_url").toString();
    }
}
