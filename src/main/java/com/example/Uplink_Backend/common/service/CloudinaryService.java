package com.example.Uplink_Backend.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);
    private static final long MAX_RAW_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB Cloudinary raw tier limit

    private final Cloudinary cloudinary;
    private final FileStorageService fileStorageService;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    public CloudinaryService(Cloudinary cloudinary, FileStorageService fileStorageService) {
        this.cloudinary = cloudinary;
        this.fileStorageService = fileStorageService;
    }

    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank() && !"placeholder".equalsIgnoreCase(cloudName)
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    /**
     * Upload raw document (PDF, DOCX) to Cloudinary raw tier with strict 10MB limit and preserved extension.
     */
    public String uploadRawFile(byte[] fileBytes, String originalFileName, String folderPath) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Cannot upload empty file payload");
        }

        if (fileBytes.length > MAX_RAW_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB Cloudinary raw file limit. Current size: " + (fileBytes.length / (1024 * 1024)) + "MB");
        }

        if (!isConfigured()) {
            logger.warn("Cloudinary not configured. Falling back to local file storage for: {}", originalFileName);
            return fileStorageService.storeBytes(fileBytes, originalFileName, folderPath);
        }

        try {
            String extension = "";
            int extIdx = originalFileName != null ? originalFileName.lastIndexOf('.') : -1;
            if (extIdx >= 0) {
                extension = originalFileName.substring(extIdx);
            }
            if (extension.isBlank()) {
                extension = ".pdf";
            }

            String baseName = originalFileName != null && extIdx > 0
                    ? originalFileName.substring(0, extIdx).replaceAll("[^a-zA-Z0-9_-]", "_")
                    : "document";

            String publicId = folderPath + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + baseName + extension;

            Map<String, Object> params = new HashMap<>();
            params.put("resource_type", "raw");
            params.put("public_id", publicId);
            params.put("overwrite", true);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(fileBytes, params);
            String secureUrl = (String) result.get("secure_url");
            logger.info("Uploaded raw file to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (IOException e) {
            logger.error("Cloudinary raw upload error: {}", e.getMessage(), e);
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Upload MultipartFile as raw file.
     */
    public String uploadRawMultipart(MultipartFile file, String folderPath) {
        try {
            return uploadRawFile(file.getBytes(), file.getOriginalFilename(), folderPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not read multipart file bytes: " + e.getMessage(), e);
        }
    }

    /**
     * Upload profile avatar image to Cloudinary image tier.
     */
    public String uploadAvatarImage(MultipartFile file, UUID userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar image file cannot be empty");
        }

        String folder = "uplink/avatars/" + userId;

        if (!isConfigured()) {
            logger.warn("Cloudinary not configured. Storing avatar in local storage.");
            return fileStorageService.storeFile(file, "avatars/" + userId);
        }

        try {
            String publicId = folder + "/avatar_" + System.currentTimeMillis();
            Map<String, Object> params = new HashMap<>();
            params.put("resource_type", "image");
            params.put("public_id", publicId);
            params.put("overwrite", true);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), params);
            String secureUrl = (String) result.get("secure_url");
            logger.info("Uploaded avatar to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (IOException e) {
            logger.error("Cloudinary avatar upload error: {}", e.getMessage(), e);
            throw new RuntimeException("Avatar upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete an asset from Cloudinary safely.
     */
    public void deleteAsset(String fileUrl, String resourceType) {
        if (fileUrl == null || !fileUrl.contains("cloudinary.com") || !isConfigured()) {
            return;
        }
        try {
            // Extract public_id from Cloudinary URL
            String sub = fileUrl.substring(fileUrl.indexOf("/upload/") + 8);
            if (sub.startsWith("v") && sub.contains("/")) {
                sub = sub.substring(sub.indexOf('/') + 1);
            }
            Map<String, Object> params = new HashMap<>();
            params.put("resource_type", resourceType != null ? resourceType : "raw");
            cloudinary.uploader().destroy(sub, params);
            logger.info("Destroyed Cloudinary asset: {}", sub);
        } catch (Exception e) {
            logger.warn("Could not delete Cloudinary asset {}: {}", fileUrl, e.getMessage());
        }
    }
}
