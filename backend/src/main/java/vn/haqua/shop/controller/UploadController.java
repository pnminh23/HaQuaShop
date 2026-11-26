package vn.haqua.shop.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.haqua.shop.exception.ApiException;

/** Tải ảnh sản phẩm (WebP/JPEG/PNG, ≤ 8 MB) lên thư mục local; prod nên đổi sang object storage. */
@RestController
@RequestMapping("/api/admin/uploads")
public class UploadController {

    private static final Set<String> ALLOWED = Set.of("image/webp", "image/jpeg", "image/png");
    private static final long MAX_BYTES = 8 * 1024 * 1024;

    private final Path root;

    UploadController(@Value("${haqua.upload-dir:uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath();
    }

    @PostMapping
    Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || !ALLOWED.contains(file.getContentType())) {
            throw ApiException.badRequest("Chỉ nhận ảnh WebP, JPEG hoặc PNG");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Ảnh vượt quá 8 MB");
        }
        String ext = switch (file.getContentType()) {
            case "image/webp" -> ".webp";
            case "image/png" -> ".png";
            default -> ".jpg";
        };
        Files.createDirectories(root);
        String name = UUID.randomUUID() + ext;
        file.transferTo(root.resolve(name));
        return Map.of("url", "/uploads/" + name);

    }
}
