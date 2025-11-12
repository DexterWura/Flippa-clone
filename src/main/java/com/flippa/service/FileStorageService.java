package com.flippa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileStorageService {
    
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)([KMGT]?B)", Pattern.CASE_INSENSITIVE);
    
    private long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 10 * 1024 * 1024; // Default 10MB
        }
        
        try {
            // Try direct parse first
            return Long.parseLong(sizeStr);
        } catch (NumberFormatException e) {
            // Try to parse with units (e.g., "10MB")
            Matcher matcher = SIZE_PATTERN.matcher(sizeStr.trim());
            if (matcher.matches()) {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toUpperCase();
                switch (unit) {
                    case "KB": return value * 1024;
                    case "MB": return value * 1024 * 1024;
                    case "GB": return value * 1024 * 1024 * 1024;
                    case "TB": return value * 1024L * 1024 * 1024 * 1024;
                    default: return value;
                }
            }
            return 10 * 1024 * 1024; // Default fallback
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadDirectory;
    private final long maxFileSize;
    private final String[] allowedImageTypes;
    
    public FileStorageService(
            @Value("${app.upload.directory:./uploads}") String uploadDir,
            @Value("${spring.servlet.multipart.max-file-size:10485760}") String maxFileSizeStr,
            @Value("${app.upload.allowed-image-types:image/jpeg,image/png,image/gif,image/webp}") String allowedTypes) {
        // Parse max file size (handles "10MB", "10485760", etc.)
        this.maxFileSize = parseSize(maxFileSizeStr);
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedImageTypes = allowedTypes.split(",");
        
        try {
            Files.createDirectories(this.uploadDirectory);
            logger.info("File upload directory initialized: {}", this.uploadDirectory);
        } catch (IOException e) {
            logger.error("Could not create upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    
    public String storeFile(MultipartFile file, String subdirectory) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        // Validate file type for images
        String contentType = file.getContentType();
        boolean isAllowed = false;
        for (String allowedType : allowedImageTypes) {
            if (contentType != null && contentType.equals(allowedType.trim())) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new RuntimeException("File type not allowed. Allowed types: " + String.join(", ", allowedImageTypes));
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Create subdirectory if it doesn't exist
        Path targetDirectory = uploadDirectory.resolve(subdirectory);
        Files.createDirectories(targetDirectory);
        
        // Save file
        Path targetPath = targetDirectory.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for storage in database
        String relativePath = subdirectory + "/" + uniqueFilename;
        logger.info("File stored: {}", relativePath);
        return relativePath;
    }
    
    public void deleteFile(String filePath) {
        try {
            Path path = uploadDirectory.resolve(filePath);
            Files.deleteIfExists(path);
            logger.info("File deleted: {}", filePath);
        } catch (IOException e) {
            logger.warn("Could not delete file: {}", filePath, e);
        }
    }
    
    public Path getFilePath(String filePath) {
        return uploadDirectory.resolve(filePath);
    }
    
    public boolean fileExists(String filePath) {
        return Files.exists(uploadDirectory.resolve(filePath));
    }
}

