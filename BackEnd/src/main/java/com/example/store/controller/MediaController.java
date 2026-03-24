package com.example.store.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.dto.MediaDeleteResponse;
import com.example.store.dto.MediaUploadResponse;
import com.example.store.exception.FileStorageException;
import com.example.store.exception.InvalidFileException;
import com.example.store.service.FileStorageService;

/**
 * REST controller for handling file uploads and serving media files
 * Provides endpoints for uploading, viewing and deleting media files
 */
@RestController
@RequestMapping("/media")
public class MediaController {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ALLOWED_CONTENT_TYPE_PREFIX = "image/";
    
    // Error messages
    private static final String ERROR_NO_FILE = "Please select a file to upload";
    private static final String ERROR_INVALID_FILE_TYPE = "Only image files are allowed";
    private static final String ERROR_FILE_TOO_LARGE = "File size must not exceed 5MB";
    private static final String ERROR_UPLOAD_FAILED = "Could not upload file";
    private static final String ERROR_DELETE_FAILED = "Could not delete file";
    
    // Success messages
    private static final String SUCCESS_UPLOAD = "File uploaded successfully";
    private static final String SUCCESS_DELETE = "File deleted successfully";

    private final FileStorageService fileStorageService;

    public MediaController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload file (Admin only)
     * POST /media/upload
     * @param file MultipartFile to upload (must be an image, max 5MB)
     * @return MediaUploadResponse with file URL and message
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MediaUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        // Validate file is not empty
        if (file.isEmpty()) {
            logger.warn("Upload attempt with empty file");
            return ResponseEntity.badRequest()
                    .body(new MediaUploadResponse(null, ERROR_NO_FILE));
        }

        // Validate file type (images only)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(ALLOWED_CONTENT_TYPE_PREFIX)) {
            logger.warn("Upload attempt with invalid content type: {}", contentType);
            return ResponseEntity.badRequest()
                    .body(new MediaUploadResponse(null, ERROR_INVALID_FILE_TYPE));
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.warn("Upload attempt with oversized file: {} bytes", file.getSize());
            return ResponseEntity.badRequest()
                    .body(new MediaUploadResponse(null, ERROR_FILE_TOO_LARGE));
        }

        try {
            String fileUrl = fileStorageService.storeFile(file);
            logger.info("File uploaded successfully: {}", fileUrl);
            return ResponseEntity.ok(new MediaUploadResponse(fileUrl, SUCCESS_UPLOAD));
        } catch (InvalidFileException e) {
            logger.error("Invalid file upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MediaUploadResponse(null, e.getMessage()));
        } catch (FileStorageException e) {
            logger.error("File storage error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MediaUploadResponse(null, ERROR_UPLOAD_FAILED + ": " + e.getMessage()));
        }
    }

    /**
     * Download/View file (Public access)
     * GET /media/{fileName}
     * @param fileName Name of the file to download
     * @return File as Resource with appropriate content type
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("File not found or not readable: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            logger.debug("Serving file: {} with content type: {}", fileName, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (InvalidFileException e) {
            logger.warn("Invalid file request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException ex) {
            logger.error("Error serving file: {}", fileName, ex);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete file (Admin only)
     * DELETE /media/{fileName}
     * @param fileName Name of the file to delete
     * @return MediaDeleteResponse with success or error message
     */
    @DeleteMapping("/{fileName:.+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MediaDeleteResponse> deleteFile(@PathVariable String fileName) {
        try {
            fileStorageService.deleteFile(fileName);
            logger.info("File deleted via API: {}", fileName);
            return ResponseEntity.ok(new MediaDeleteResponse(SUCCESS_DELETE));
        } catch (InvalidFileException e) {
            logger.warn("Invalid file deletion attempt: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MediaDeleteResponse(e.getMessage()));
        } catch (FileStorageException e) {
            logger.error("Error deleting file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MediaDeleteResponse(ERROR_DELETE_FAILED + ": " + e.getMessage()));
        }
    }
}
