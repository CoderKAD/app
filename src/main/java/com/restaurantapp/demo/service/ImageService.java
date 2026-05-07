package com.restaurantapp.demo.service;

import com.restaurantapp.demo.entity.MenuItem;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private static final String IMAGE_DIR = "uploads/menu-items";

    private final Tika tika = new Tika();
    private final Path root = Paths.get(IMAGE_DIR);

    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        validate(file);
        ensureDir();

        // Reset input stream for tika.detect after validation
        String mime = tika.detect(file.getBytes());

        String ext = switch (mime) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IllegalArgumentException("Unsupported image format: " + mime);
        };

        String fileName = UUID.randomUUID() + ext;
        Path target = root.resolve(fileName);

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/menu-items/" + fileName;
    }

    public void delete(String url) {
        if (url == null || url.isBlank()) return;

        try {
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            Files.deleteIfExists(root.resolve(fileName));
        } catch (Exception e) {
            System.err.println("Delete error: " + e.getMessage());
        }
    }

    private void validate(MultipartFile file) throws IOException {
        if (file.getSize() > 10L * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("File too large (max 10MB)");
        }

        String mime = tika.detect(file.getBytes());
        if (!List.of("image/jpeg", "image/jpg", "image/png").contains(mime)) {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed");
        }
    }

    private void ensureDir() throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
    }

    public void updateImageIfNeeded(MenuItem existing, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            deleteImageIfExists(existing.getImageUrl());
            String newImageUrl = upload(image);
            if (newImageUrl != null) {
                existing.setImageUrl(newImageUrl);
            }
        }
    }

    public void deleteImageIfExists(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isBlank()) {
            delete(imageUrl);
        }
    }

    public String uploadImageIfPresent(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        return upload(image);
    }
}