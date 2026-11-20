package com.example.Uplink_Backend.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path fileStorageLocation;

    public FileStorageService(
            @Value("${uplink.storage.upload-dir:${java.io.tmpdir}/uplink-uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Could not create upload directory: {}", this.fileStorageLocation, ex);
        }
    }

    public String storeFile(MultipartFile file, String subfolder) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String fileExtension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFilename.substring(i);
        }

        String safeSubfolder = (subfolder != null && !subfolder.isBlank()) ? subfolder : "general";
        String fileName = UUID.randomUUID() + fileExtension;

        try {
            Path targetFolder = this.fileStorageLocation.resolve(safeSubfolder).normalize();
            if (!targetFolder.startsWith(this.fileStorageLocation)) {
                throw new IllegalArgumentException("Invalid subfolder path");
            }
            Files.createDirectories(targetFolder);

            Path targetLocation = targetFolder.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/api/files/download/" + safeSubfolder + "/" + fileName;
        } catch (IOException ex) {
            logger.error("Failed to store file: {}", fileName, ex);
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public String storeBytes(byte[] bytes, String originalFilename, String subfolder) {
        String fileExtension = "";
        int i = originalFilename != null ? originalFilename.lastIndexOf('.') : -1;
        if (i > 0) {
            fileExtension = originalFilename.substring(i);
        }

        String safeSubfolder = (subfolder != null && !subfolder.isBlank()) ? subfolder.replace('\\', '/') : "general";
        String fileName = UUID.randomUUID() + fileExtension;

        try {
            Path targetFolder = this.fileStorageLocation.resolve(safeSubfolder).normalize();
            if (!targetFolder.startsWith(this.fileStorageLocation)) {
                throw new IllegalArgumentException("Invalid subfolder path");
            }
            Files.createDirectories(targetFolder);

            Path targetLocation = targetFolder.resolve(fileName);
            Files.write(targetLocation, bytes);

            return "/api/files/download/" + safeSubfolder + "/" + fileName;
        } catch (IOException ex) {
            logger.error("Failed to store byte file: {}", fileName, ex);
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public byte[] loadFileAsBytes(String relativePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                logger.warn("Blocked path traversal attempt: {}", relativePath);
                return new byte[0];
            }
            File file = filePath.toFile();
            if (file.exists()) {
                return Files.readAllBytes(filePath);
            }
            return new byte[0];
        } catch (IOException e) {
            logger.error("Error reading file: {}", relativePath, e);
            return new byte[0];
        }
    }
}