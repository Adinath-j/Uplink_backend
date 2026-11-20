package com.example.Uplink_Backend.common.controller;

import com.example.Uplink_Backend.common.dto.ApiResponse;
import com.example.Uplink_Backend.common.security.UserPrincipal;
import com.example.Uplink_Backend.common.service.CloudinaryService;
import com.example.Uplink_Backend.common.service.FileStorageService;
import com.example.Uplink_Backend.modules.ocr.entity.Document;
import com.example.Uplink_Backend.modules.ocr.repository.DocumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Cross-Cutting: File Storage & Documents", description = "Endpoints for downloading, uploading, and managing stored files and documents")
public class FileController {

    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    private final DocumentRepository documentRepository;

    public FileController(FileStorageService fileStorageService,
                          CloudinaryService cloudinaryService,
                          DocumentRepository documentRepository) {
        this.fileStorageService = fileStorageService;
        this.cloudinaryService = cloudinaryService;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/api/files/download/**")
    @Operation(summary = "Download or stream stored file by relative path")
    public ResponseEntity<byte[]> downloadFile(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String relativePath = fullPath.substring(fullPath.indexOf("/download/") + "/download/".length());

        byte[] data = fileStorageService.loadFileAsBytes(relativePath);
        if (data.length == 0) {
            return ResponseEntity.notFound().build();
        }

        String filename = relativePath.contains("/") ? relativePath.substring(relativePath.lastIndexOf('/') + 1) : relativePath;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping(value = "/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload raw document or file to Cloudinary / Local Storage")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @RequestParam(value = "resourceType", defaultValue = "raw") String resourceType) {

        String fileUrl;
        if ("image".equalsIgnoreCase(resourceType)) {
            fileUrl = cloudinaryService.isConfigured()
                    ? cloudinaryService.uploadAvatarImage(file, UUID.randomUUID())
                    : fileStorageService.storeFile(file, folder);
        } else {
            fileUrl = cloudinaryService.uploadRawMultipart(file, "uplink/" + folder);
        }

        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", Map.of("fileUrl", fileUrl)));
    }

    @GetMapping("/api/documents")
    @Operation(summary = "Get user documents with optional type filter (e.g. RESUME)")
    public ResponseEntity<ApiResponse<List<Document>>> getUserDocuments(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "type", required = false) String type) {

        List<Document> docs = documentRepository.findByUserIdOrderByUploadedAtDesc(userPrincipal.getId());
        if (type != null && !type.isBlank()) {
            docs = docs.stream()
                    .filter(d -> type.equalsIgnoreCase(d.getType()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.ok("Documents retrieved", docs));
    }

    @DeleteMapping("/api/documents/{id}")
    @Operation(summary = "Delete user document by id")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {

        Document doc = documentRepository.findById(id).orElse(null);
        if (doc != null && doc.getUser().getId().equals(userPrincipal.getId())) {
            cloudinaryService.deleteAsset(doc.getFileUrl(), "raw");
            documentRepository.delete(doc);
        }
        return ResponseEntity.ok(ApiResponse.ok("Document deleted", null));
    }
}
