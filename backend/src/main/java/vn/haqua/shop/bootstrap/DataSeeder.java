package vn.haqua.shop.bootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.entity.Banner;
import vn.haqua.shop.entity.Category;
import vn.haqua.shop.entity.ChatbotConfig;
import vn.haqua.shop.entity.Coupon;
import vn.haqua.shop.entity.KnowledgeSource;
import vn.haqua.shop.entity.Post;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.entity.ProductImage;
import vn.haqua.shop.entity.ProductSpec;
import vn.haqua.shop.entity.ProductVariant;
import vn.haqua.shop.entity.SeoMeta;
import vn.haqua.shop.entity.User;
import vn.haqua.shop.enums.ChatEnums.KnowledgeType;
import vn.haqua.shop.enums.CouponType;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.enums.Role;
import vn.haqua.shop.repository.BannerRepository;
import vn.haqua.shop.repository.CategoryRepository;
import vn.haqua.shop.repository.ChatbotConfigRepository;
import vn.haqua.shop.repository.CouponRepository;
import vn.haqua.shop.repository.KnowledgeSourceRepository;
import vn.haqua.shop.repository.PostRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.repository.UserRepository;
import vn.haqua.shop.service.SeoService;
import vn.haqua.shop.util.SlugUtil;

/** Dữ liệu mẫu cho môi trường dev (haqua.seed.enabled=true), lấy theo bản thiết kế. */
@Component
@org.springframework.core.annotation.Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "haqua.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CouponRepository coupons;
    private final KnowledgeSourceRepository knowledge;
    private final ChatbotConfigRepository chatConfigs;
    private final PostRepository posts;
    private final PasswordEncoder encoder;
    private final SeoService seoService;
    private final BannerRepository banners;

    @Value("${haqua.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        seedBanners();
        seedUsers();
        Category laptop = categories.save(new Category("Máy tính", "may-tinh",
                "Laptop học tập, văn phòng, đồ họa chính hãng, bảo hành 12 tháng.", 1));
        Category phone = categories.save(new Category("Điện thoại", "dien-thoai",
                "Điện thoại từ tầm trung đến flagship, trả góp 0%.", 2));
        Category audio = categories.save(new Category("Tai nghe", "tai-nghe",
                "Tai nghe chống ồn, true wireless và thể thao.", 3));

        // Nova X 5G: 3 màu × 3 dung lượng, giá đổi theo dung lượng (như màn Chi tiết)
        Product nova = product(phone, "Điện thoại Nova X 5G", "Nova", 12, 4.8, 342, 1200,
                "Nova X 5G có màn hình AMOLED 6,7 inch 120Hz, pin 5.000 mAh, sạc nhanh 67W, camera chính 50MP chống rung quang học.");
        String[][] colors = {{"Xanh rêu", "XR"}, {"Đen than", "DT"}, {"Bạc sương", "BS"}};
        long[][] storages = {{128, 19_990_000, 21_490_000}, {256, 21_990_000, 23_990_000}, {512, 25_990_000, 27_990_000}};
        for (String[] c : colors) {
            for (long[] s : storages) {
                nova.getVariants().add(new ProductVariant(nova, "HQ-NOVAX-" + s[0] + "-" + c[1], c[0], s[0] + "GB", s[1], s[2], 20));
            }
        }
        spec(nova, "Màn hình", "AMOLED 6,7 inch, 2400×1080, 120Hz", "Chip xử lý", "8 nhân, tiến trình 4nm", "RAM", "8GB",
                "Camera sau", "50MP (OIS) + 12MP góc siêu rộng", "Pin & sạc", "5.000 mAh, sạc nhanh 67W",
                "Kết nối", "5G, Wi-Fi 6E, Bluetooth 5.3, NFC", "Kháng nước", "IP68");
        finish(nova, "điện thoại nova x", "Mua điện thoại Nova X 5G chính hãng – giá tốt, trả góp 0% | HaQua",
                "Điện thoại Nova X 5G màn AMOLED 120Hz, pin 5.000 mAh, sạc nhanh 67W, camera 50MP. Giao nhanh 2h, đổi trả 30 ngày, bảo hành 12 tháng.");

        simple(laptop, "Laptop Aero 14 OLED – 16GB/512GB", "Aero", 24_990_000, 27_490_000, 15, 4.8, 126, 400, "AERO14");
        simple(phone, "Điện thoại Nova Lite – 128GB", "Nova", 6_490_000, 0, 30, 4.5, 189, 600, "NOVALITE");
        simple(audio, "Tai nghe chống ồn Silence ANC", "Silence", 5_990_000, 7_490_000, 25, 4.9, 264, 900, "SILENCE");
        simple(laptop, "Laptop Creator 16 – RTX, 32GB/1TB", "Creator", 42_990_000, 0, 6, 4.9, 58, 80, "CREATOR16");
        simple(audio, "Tai nghe True Wireless Pod 3", "Pod", 2_490_000, 2_990_000, 60, 4.7, 401, 1500, "POD3");
        simple(phone, "Điện thoại Terra Pro – 512GB", "Terra", 28_990_000, 31_990_000, 12, 4.7, 97, 200, "TERRAPRO");
        simple(laptop, "Laptop Văn phòng Lite 15 – 8GB/256GB", "Lite", 13_490_000, 14_990_000, 25, 4.6, 211, 500, "LITE15");
        simple(audio, "Tai nghe Studio Over-ear", "Studio", 3_990_000, 0, 18, 4.6, 77, 150, "STUDIO");
        simple(laptop, "Laptop Air Book 13 – 16GB/512GB", "Air", 19_990_000, 0, 14, 4.7, 133, 300, "AIRBOOK13");
        simple(phone, "Điện thoại gập Fold Z – 256GB", "Fold", 34_990_000, 0, 4, 4.6, 41, 60, "FOLDZ");
        simple(audio, "Tai nghe Thể thao Run Neckband", "Run", 990_000, 1_290_000, 80, 4.4, 156, 700, "RUN");

        Instant now = Instant.now();
        Coupon c = new Coupon();
        c.setCode("HAQUA10");
        c.setType(CouponType.PERCENT);
        c.setValue(10);
        c.setMaxDiscount(2_000_000);
        c.setMinOrder(500_000);
        c.setStartAt(now.minus(1, ChronoUnit.DAYS));
        c.setEndAt(now.plus(365, ChronoUnit.DAYS));
        c.setUsageLimit(1000);
        coupons.save(c);

        chatConfigs.save(new ChatbotConfig());
        knowledge.saveAll(List.of(
                new KnowledgeSource(KnowledgeType.CATALOG, "Danh mục sản phẩm", "Giá và tồn kho lấy trực tiếp từ hệ thống."),
                new KnowledgeSource(KnowledgeType.POLICY, "Chính sách đổi trả và bảo hành",
                        "Đổi trả trong 30 ngày nếu lỗi do nhà sản xuất; bảo hành chính hãng 12 tháng. Đổi trả hoàn tiền theo chính sách."),
                new KnowledgeSource(KnowledgeType.POLICY, "Chính sách giao hàng",
                        "Miễn phí giao hàng cho đơn từ 500.000₫. Giao nhanh 2 giờ tại Hà Nội và TP.HCM (+30.000₫)."),
                new KnowledgeSource(KnowledgeType.FAQ, "Mua trả góp 0% như thế nào?",
                        "Chọn Trả góp 0% khi thanh toán, qua thẻ tín dụng của các ngân hàng liên kết, kỳ hạn 3–12 tháng."),
                new KnowledgeSource(KnowledgeType.FAQ, "Bao lâu thì nhận được hàng?",
                        "Giao tiêu chuẩn 1–3 ngày; giao nhanh 2 giờ tại Hà Nội và TP.HCM."),
                new KnowledgeSource(KnowledgeType.FAQ, "Có xuất hóa đơn VAT không?",
                        "Có. Bật 'Xuất hóa đơn điện tử cho công ty' khi thanh toán và nhập mã số thuế."),
                new KnowledgeSource(KnowledgeType.ORDER_LOOKUP, "Tra cứu đơn hàng", "Tra bằng mã đơn và số điện thoại.")));

        Post post = new Post();
        post.setTitle("Cách chọn laptop cho sinh viên năm nhất");
        post.setSlug("cach-chon-laptop-cho-sinh-vien");
        post.setTag("Laptop");
        post.setContent("Ngân sách, cân nặng, thời lượng pin và RAM tối thiểu 16GB là những tiêu chí quan trọng nhất khi chọn laptop học tập.");
        post.setPublishedAt(now);
        posts.save(post);
    }

    private void seedUsers() {
        users.save(new User("admin@haqua.vn", "0900000001", encoder.encode("Admin@123"), "Quản trị viên", Role.ADMIN));
        users.save(new User("staff@haqua.vn", "0900000002", encoder.encode("Staff@123"), "Nhân viên CSKH", Role.STAFF));
        users.save(new User("khach@haqua.vn", "0900000003", encoder.encode("Khach@123"), "Khách Demo", Role.CUSTOMER));
    }

    private Product product(Category cat, String name, String brand, int sold, double rating, int reviews, int unused, String desc) {
        Product p = new Product();
        p.setCategory(cat);
        p.setBrand(brand);
        p.rename(name);
        p.setSlug(SlugUtil.toSlug(name));
        p.setDescription(desc);
        p.setStatus(ProductStatus.ACTIVE);
        p.setSoldCount(unused > 0 ? unused : sold);
        p.setRatingAvg(rating);
        p.setReviewCount(reviews);
        return p;
    }

    private void spec(Product p, String... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) {
            p.getSpecs().add(new ProductSpec(p, kv[i], kv[i + 1]));
        }
    }

    private void finish(Product p, String keyword, String title, String description) {
        SeoMeta seo = new SeoMeta(p);
        seo.setFocusKeyword(keyword);
        seo.setMetaTitle(title);
        seo.setMetaDescription(description);
        seo.setSeoScore(seoService.evaluate(keyword, p.getSlug(), title, description, List.of()).score());
        p.setSeo(seo);
        p.getImages().add(new ProductImage(p, placeholderImage(p), p.getName(), 0));
        p.refreshDerived();
        products.save(p);
    }

    private void simple(Category cat, String name, String brand, long price, long original, int stock,
                        double rating, int reviews, int sold, String sku) {
        Product p = product(cat, name, brand, sold, rating, reviews, 0, name + " chính hãng, bảo hành 12 tháng, đổi trả trong 30 ngày.");
        p.getVariants().add(new ProductVariant(p, "HQ-" + sku, null, null, price, original, stock));
        finish(p, name.toLowerCase(), name + " chính hãng | HaQua", name + " chính hãng, giá tốt tại HaQua.");

    }

    /** Ảnh minh họa SVG cho dữ liệu mẫu dev (ảnh thật do admin tải lên qua /api/admin/uploads). */
    private String placeholderImage(Product p) {
        String name = p.getName().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String[] tones = {"#cfdcc9", "#e7ece2", "#d9e4d2", "#c5d6bf"};
        String tone = tones[(int) (Math.abs(p.getSlug().hashCode()) % tones.length)];
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 600\"><rect width=\"800\" height=\"600\" fill=\"" + tone
                + "\"/><rect x=\"260\" y=\"140\" width=\"280\" height=\"240\" rx=\"24\" fill=\"#2c4231\"/><rect x=\"280\" y=\"160\" width=\"240\" height=\"200\" rx=\"10\" fill=\"#e7ece2\"/>"
                + "<text x=\"400\" y=\"470\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"34\" fill=\"#2c4231\">" + name + "</text></svg>";
        String file = "seed-" + p.getSlug() + ".svg";
        try {
            Path dir = Path.of(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(file), svg, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "/uploads/" + file;
    }

    /** 3 slide mẫu (ảnh SVG phối màu thương hiệu); admin thay bằng ảnh thật tại /admin/banner. */
    private void seedBanners() {
        String[][] data = {
            {"Công nghệ tinh gọn cho nhịp sống xanh", "Máy tính, điện thoại và tai nghe chính hãng — giá minh bạch, giao nhanh, bảo hành tận tâm.", "Xem máy tính", "/san-pham?category=may-tinh", "#2c4231", "#4f7355", "laptop"},
            {"Điện thoại mới, ưu đãi đến 7%", "Nova X 5G cùng loạt điện thoại chính hãng, trả góp 0% qua thẻ tín dụng.", "Xem điện thoại", "/san-pham?category=dien-thoai", "#3e5a43", "#7da07f", "phone"},
            {"Âm thanh trọn vẹn, chống ồn chủ động", "Tai nghe True Wireless và ANC được chọn lọc, đổi trả trong 30 ngày.", "Khám phá tai nghe", "/san-pham?category=tai-nghe", "#34503a", "#a7c3a0", "audio"},
        };
        int order = 0;
        for (String[] d : data) {
            Banner b = new Banner();
            b.setTitle(d[0]);
            b.setSubtitle(d[1]);
            b.setCtaLabel(d[2]);
            b.setLinkUrl(d[3]);
            b.setAltText(d[0]);
            b.setImageUrl(bannerArt(order, d[4], d[5], d[6]));
            b.setSortOrder(order++);
            banners.save(b);
        }
    }

    private String bannerArt(int n, String c1, String c2, String kind) {
        String device = switch (kind) {
            case "phone" -> "<rect x='1110' y='110' width='190' height='400' rx='34' fill='#18211b'/><rect x='1124' y='126' width='162' height='368' rx='22' fill='#e7ece2'/>"
                    + "<rect x='1250' y='170' width='170' height='360' rx='34' fill='#2c4231' opacity='.85' transform='rotate(12 1335 350)'/>";
            case "audio" -> "<path d='M1030 380v-70a190 190 0 01380 0v70' fill='none' stroke='#e7ece2' stroke-width='36' stroke-linecap='round'/>"
                    + "<rect x='990' y='360' width='96' height='170' rx='44' fill='#18211b'/><rect x='1354' y='360' width='96' height='170' rx='44' fill='#18211b'/>";
            default -> "<rect x='980' y='150' width='460' height='290' rx='22' fill='#18211b'/><rect x='1000' y='170' width='420' height='250' rx='10' fill='#e7ece2'/>"
                    + "<path d='M930 460h560l-40 50H970z' fill='#c9d8c1'/>";
        };
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1600 640' preserveAspectRatio='xMidYMid slice'>"
                + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='" + c1 + "'/><stop offset='1' stop-color='" + c2 + "'/></linearGradient></defs>"
                + "<rect width='1600' height='640' fill='url(#g)'/>"
                + "<circle cx='1260' cy='320' r='330' fill='#ffffff' opacity='.08'/><circle cx='1400' cy='90' r='150' fill='#ffffff' opacity='.07'/>"
                + "<path d='M700 640c120-160 300-210 470-190' fill='none' stroke='#ffffff' stroke-opacity='.12' stroke-width='60'/>"
                + device + "</svg>";
        String file = "seed-banner-" + n + ".svg";
        try {
            Path dir = Path.of(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(file), svg, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "/uploads/" + file;
    }
}
