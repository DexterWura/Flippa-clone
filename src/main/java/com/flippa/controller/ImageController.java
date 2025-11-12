package com.flippa.controller;

import com.flippa.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Path;

@Controller
@RequestMapping("/images")
public class ImageController {
    
    private final FileStorageService fileStorageService;
    
    public ImageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    
    @GetMapping("/{subdirectory}/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String subdirectory, @PathVariable String filename) {
        try {
            String filePath = subdirectory + "/" + filename;
            Path path = fileStorageService.getFilePath(filePath);
            
            if (!fileStorageService.fileExists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(path);
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}

