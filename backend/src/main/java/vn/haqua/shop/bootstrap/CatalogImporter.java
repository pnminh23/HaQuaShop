package vn.haqua.shop.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.entity.Category;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.entity.ProductImage;
import vn.haqua.shop.entity.ProductSpec;
import vn.haqua.shop.entity.ProductVariant;
import vn.haqua.shop.entity.SeoMeta;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.repository.CategoryRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.repository.ProductVariantRepository;
import vn.haqua.shop.service.BrandService;
import vn.haqua.shop.service.SeoService;
import vn.haqua.shop.util.SlugUtil;

/**
 * Nhập danh mục sản phẩm từ các file CSV do tools/fptshop_crawl.py tạo ra.
 * Bật bằng haqua.import.dir (vd HAQUA_IMPORT_DIR=tools/out). Chạy lặp lại an toàn: sản phẩm đã có (trùng slug) sẽ bị bỏ qua.
 * Sản phẩm nhập vào ở trạng thái NHÁP; giá lấy từ cột price_ref (giá thị trường tham chiếu), tồn kho mặc định 10.
 */
@Slf4j
@Component
@Order(20) // sau DataSeeder (đã tạo danh mục)
@RequiredArgsConstructor
public class CatalogImporter implements ApplicationRunner {

    /** file CSV -> slug danh mục của shop. */
    private static final Map<String, String> FILES = Map.of(
            "dien-thoai.csv", "dien-thoai",
            "may-tinh-xach-tay.csv", "may-tinh",
            "tai-nghe.csv", "tai-nghe",
            "chuot.csv", "chuot",
            "ban-phim.csv", "ban-phim");

    /** Danh mục DataSeeder không tạo (DB đã có dữ liệu thì seeder bỏ qua) – tạo khi nhập. */
    private static final Map<String, Category> EXTRA_CATEGORIES = Map.of(
            "chuot", new Category("Chuột", "chuot", "Chuột không dây, Bluetooth và gaming chính hãng.", 4),
            "ban-phim", new Category("Bàn phím", "ban-phim", "Bàn phím cơ, không dây và gaming chính hãng.", 5));

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final BrandService brandService;
    private final SeoService seoService;
    private final ObjectMapper json;

    @Value("${haqua.import.dir:}")
    private String importDir;

    @Value("${haqua.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (importDir == null || importDir.isBlank()) return;
        Path dir = resolveDir();
        if (dir == null) {
            log.warn("Không tìm thấy thư mục dữ liệu nhập '{}' (cwd = {}). Đặt HAQUA_IMPORT_DIR=tools/out nếu chạy từ thư mục gốc, "
                    + "hoặc ../tools/out nếu chạy từ backend.", importDir, Path.of("").toAbsolutePath());
            return;
        }
        int added = 0;
        int skipped = 0;
        int priced = 0;
        int galleries = 0;
        for (var f : FILES.entrySet()) {
            Path csv = dir.resolve(f.getKey());
            Category cat = Files.exists(csv) ? categoryFor(f.getValue()) : null;
            if (cat == null) {
                log.warn("Bỏ qua {}: thiếu file hoặc danh mục '{}'", csv, f.getValue());
                continue;
            }
            for (Map<String, String> row : readCsv(csv)) {
                if (importRow(row, cat, dir)) { added++; continue; }
                boolean g = applyGallery(row, dir);
                boolean pr = applyPrice(row);
                if (g) galleries++;
                if (pr) priced++;
                if (!g && !pr) skipped++;
            }
        }
        log.info("Nhập danh mục từ {}: thêm {} sản phẩm, cập nhật giá {}, bổ sung gallery ảnh {}, bỏ qua {}", dir, added, priced, galleries, skipped);
    }

    private Category categoryFor(String slug) {
        return categories.findBySlug(slug).orElseGet(() -> {
            Category t = EXTRA_CATEGORIES.get(slug);
            return t == null ? null : categories.save(new Category(t.getName(), t.getSlug(), t.getSeoDescription(), t.getSortOrder()));
        });
    }

    /** Chịu được việc chạy từ thư mục gốc hay từ backend: thử đường dẫn đã cấu hình rồi các vị trí thường gặp. */
    private Path resolveDir() {
        for (String c : List.of(importDir, "tools/out", "../tools/out")) {
            Path d = Path.of(c).toAbsolutePath().normalize();
            if (Files.isDirectory(d) && FILES.keySet().stream().anyMatch(f -> Files.exists(d.resolve(f)))) return d;
        }
        return null;
    }

    /** Giá tham chiếu thị trường (price_ref) và tồn kho mặc định cho biến thể chưa được đặt giá; không ghi đè giá admin đã sửa. */
    private static final int DEFAULT_STOCK = 10;

    private static long priceOf(Map<String, String> r) {
        try { return Long.parseLong(r.getOrDefault("price_ref", "0").trim()); } catch (NumberFormatException e) { return 0; }
    }

    private boolean applyPrice(Map<String, String> r) {
        long price = priceOf(r);
        String url = r.getOrDefault("source_url", "");
        String slug = url.isBlank() ? SlugUtil.toSlug(r.getOrDefault("name", "")) : url.substring(url.lastIndexOf('/') + 1);
        if (price <= 0) return false;
        Product p = products.findBySlug(slug).orElse(null);
        if (p == null || p.getVariants().size() != 1) return false;
        ProductVariant v = p.getVariants().get(0);
        if (v.getPrice() > 0) return false;
        v.setPrice(price);
        v.setStock(DEFAULT_STOCK);
        p.refreshDerived();
        return true;
    }

    private boolean importRow(Map<String, String> r, Category cat, Path dir) throws IOException {
        String name = r.getOrDefault("name", "").trim();
        String url = r.getOrDefault("source_url", "");
        String slug = url.isBlank() ? SlugUtil.toSlug(name) : url.substring(url.lastIndexOf('/') + 1);
        String sku = "FPT-" + r.getOrDefault("sku_source", slug);
        if (name.isEmpty() || slug.isEmpty() || products.existsBySlug(slug) || variants.existsBySku(sku)) return false;

        Product p = new Product();
        p.setCategory(cat);
        String brand = r.getOrDefault("brand", "").trim();
        if (!brand.isEmpty()) p.setBrand(brandService.ensure(brand).getName());
        p.rename(name);
        p.setSlug(slug);
        p.setStatus(ProductStatus.DRAFT);

        Map<String, String> specs = r.getOrDefault("specs_json", "").isBlank() ? Map.of()
                : json.readValue(r.get("specs_json"), new TypeReference<LinkedHashMap<String, String>>() { });
        specs.forEach((k, v) -> { if (v != null && !v.isBlank()) p.getSpecs().add(new ProductSpec(p, k, v)); });
        String highlights = String.join(", ", specs.entrySet().stream().limit(4).map(e -> e.getKey().toLowerCase() + " " + e.getValue()).toList());
        p.setDescription(name + " chính hãng" + (highlights.isEmpty() ? "" : " – " + highlights) + ". Bảo hành 12 tháng, đổi trả trong 30 ngày.");

        p.getVariants().add(new ProductVariant(p, sku, null, null, priceOf(r), 0, priceOf(r) > 0 ? DEFAULT_STOCK : 0));

        addImages(p, imagePaths(r), dir);

        String title = name + " chính hãng | HaQua";
        String desc = name + " chính hãng, giá tốt tại HaQua. Giao nhanh, đổi trả trong 30 ngày.";
        SeoMeta seo = new SeoMeta(p);
        seo.setFocusKeyword(name.toLowerCase());
        seo.setMetaTitle(title);
        seo.setMetaDescription(desc);
        seo.setSeoScore(seoService.evaluate(seo.getFocusKeyword(), slug, title, desc, List.of()).score());
        p.setSeo(seo);
        p.refreshDerived();
        products.save(p);
        return true;
    }

    /** Danh sách ảnh (gallery) của dòng CSV: images_json, hoặc image_file nếu CSV cũ. */
    private List<String> imagePaths(Map<String, String> r) {
        String raw = r.getOrDefault("images_json", "");
        if (!raw.isBlank()) return json.readValue(raw, new TypeReference<List<String>>() { });
        String one = r.getOrDefault("image_file", "");
        return one.isBlank() ? List.of() : List.of(one);
    }

    private void addImages(Product p, List<String> rels, Path dir) throws IOException {
        int order = 0;
        for (String rel : rels) {
            String url = copyImage(rel, p.getSlug() + "-" + (order + 1), dir);
            if (url != null) p.getImages().add(new ProductImage(p, url, p.getName() + " – ảnh " + (order + 1), order++));
        }
    }

    /** Sản phẩm đã nhập trước đây chỉ có 1 ảnh nhập sẵn: bổ sung đủ gallery (không đụng ảnh admin đã tự tải lên). */
    private boolean applyGallery(Map<String, String> r, Path dir) throws IOException {
        List<String> rels = imagePaths(r);
        if (rels.size() < 2) return false;
        String url = r.getOrDefault("source_url", "");
        String slug = url.isBlank() ? SlugUtil.toSlug(r.getOrDefault("name", "")) : url.substring(url.lastIndexOf('/') + 1);
        Product p = products.findBySlug(slug).orElse(null);
        if (p == null || p.getImages().size() >= rels.size()) return false;
        boolean untouched = p.getImages().stream().allMatch(i -> i.getUrl() != null && i.getUrl().startsWith("/uploads/import-"));
        if (!untouched) return false;
        p.getImages().clear();
        addImages(p, rels, dir);
        return true;
    }

    private String copyImage(String rel, String baseName, Path dir) throws IOException {
        if (rel.isBlank()) return null;
        Path src = dir.resolve(rel);
        if (!Files.isRegularFile(src)) return null;
        String file = src.getFileName().toString();
        String ext = file.contains(".") ? file.substring(file.lastIndexOf('.')) : ".png";
        Path root = Path.of(uploadDir).toAbsolutePath();
        Files.createDirectories(root);
        String name = "import-" + baseName + ext;
        Files.copy(src, root.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + name;
    }

    /** CSV RFC 4180 tối giản: có dấu ngoặc kép, xuống dòng trong ô, BOM. Dòng đầu là tiêu đề. */
    static List<Map<String, String>> readCsv(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { sb.append('"'); i++; } else quoted = false;
                } else sb.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { cur.add(sb.toString()); sb.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                cur.add(sb.toString()); sb.setLength(0);
                if (!(cur.size() == 1 && cur.get(0).isEmpty())) rows.add(cur);
                cur = new ArrayList<>();
            } else sb.append(c);
        }
        if (sb.length() > 0 || !cur.isEmpty()) { cur.add(sb.toString()); rows.add(cur); }
        List<Map<String, String>> out = new ArrayList<>();
        if (rows.isEmpty()) return out;
        List<String> head = rows.get(0);
        for (int r = 1; r < rows.size(); r++) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int c = 0; c < head.size() && c < rows.get(r).size(); c++) m.put(head.get(c), rows.get(r).get(c));
            out.add(m);
        }
        return out;
    }
}
