package com.example.store.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.store.exception.FileStorageException;
import com.example.store.exception.InvalidFileException;
import com.example.store.exception.ResourceNotFoundException;

/**
 * Service for handling file storage operations
 * Manages file upload, retrieval and deletion
 */
@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String INVALID_PATH_SEQUENCE = "..";

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage location initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            logger.error("Failed to create upload directory: {}", this.fileStorageLocation, ex);
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Store file and return the URL to access it
     * @param file MultipartFile to store
     * @return URL to access the stored file (e.g., http://localhost:8080/media/uuid_filename.jpg)
     * @throws InvalidFileException if filename contains invalid characters
     * @throws FileStorageException if file cannot be stored
     */
    public String storeFile(MultipartFile file) {
        // Validate and clean filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        if (originalFileName.contains(INVALID_PATH_SEQUENCE)) {
            logger.warn("Invalid filename detected: {}", originalFileName);
            throw new InvalidFileException("Filename contains invalid path sequence: " + originalFileName);
        }

        // Generate unique filename with original extension
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Copy file to the target location
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("File stored successfully: {} (original: {})", uniqueFileName, originalFileName);

            // Generate URL
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/media/")
                    .path(uniqueFileName)
                    .toUriString();

            return fileUrl;
        } catch (IOException ex) {
            logger.error("Failed to store file: {}", originalFileName, ex);
            throw new FileStorageException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Load file as Path
     * @param fileName Name of the file to load
     * @return Path to the file
     * @throws InvalidFileException if filename contains invalid characters
     */
    public Path loadFile(String fileName) {
        if (fileName.contains(INVALID_PATH_SEQUENCE)) {
            logger.warn("Invalid filename in load request: {}", fileName);
            throw new InvalidFileException("Filename contains invalid path sequence: " + fileName);
        }
        
        return fileStorageLocation.resolve(fileName).normalize();
    }

    /**
     * Delete file
     * @param fileName Name of the file to delete
     * @throws InvalidFileException if filename contains invalid characters
     * @throws FileStorageException if file cannot be deleted
     */
    public void deleteFile(String fileName) {
        try {
            Path filePath = loadFile(fileName);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                logger.info("File deleted successfully: {}", fileName);
            } else {
                logger.warn("File not found for deletion: {}", fileName);
            }
        } catch (IOException ex) {
            logger.error("Failed to delete file: {}", fileName, ex);
            throw new FileStorageException("Could not delete file " + fileName, ex);
        }
    }
}

