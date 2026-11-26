package vn.haqua.shop.service.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import vn.haqua.shop.service.chat.ProductAdvisor.Advice;
import vn.haqua.shop.service.chat.ProductAdvisor.Candidate;
import vn.haqua.shop.util.SlugUtil;

/** Viết phần sản phẩm trong NGỮ CẢNH gửi cho trợ lý AI. */
public final class ProductContext {

    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put(ChatIntent.COMPUTER, "máy tính");
        CATEGORY_NAMES.put(ChatIntent.PHONE, "điện thoại");
        CATEGORY_NAMES.put(ChatIntent.AUDIO, "tai nghe");
    }

    /** Thông số được ưu tiên đưa vào (so theo đầu tên thông số, đã bỏ dấu). */
    private static final List<String> KEY_SPECS = List.of("cpu", "chip", "ram", "card do hoa", "o cung", "bo nho",
            "dung luong", "kich thuoc man hinh", "tinh nang", "kieu ket noi", "thoi luong pin", "bluetooth", "camera");
    private static final int MAX_SPECS = 7;

    private ProductContext() {
    }

    public static String describe(Advice advice, List<Candidate> catalog) {
        ChatIntent intent = advice.intent();
        if (advice.items().isEmpty() && intent.category() == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        String stock = CATEGORY_NAMES.entrySet().stream()
                .map(e -> Map.entry(e.getValue(), catalog.stream().filter(c -> e.getKey().equals(c.category())).count()))
                .filter(e -> e.getValue() > 0)
                .map(e -> e.getValue() + " " + e.getKey())
                .collect(Collectors.joining(", "));
        out.append("Shop đang bán: ").append(stock).append(". Danh sách dưới đây chỉ là phần lọc theo câu hỏi.\n");
        String conditions = conditions(intent);
        if (!conditions.isEmpty()) {
            out.append("Điều kiện của khách: ").append(conditions).append(".\n");
        }
        if (advice.items().isEmpty()) {
            return out.append("Shop hiện chưa có ").append(CATEGORY_NAMES.getOrDefault(intent.category(), "sản phẩm này"))
                    .append(" đang bán.").toString();
        }
        if (!advice.relaxed().isEmpty()) {
            out.append("Không có sản phẩm nào đáp ứng đủ các điều kiện. Danh sách là các lựa chọn gần nhất sau khi bỏ điều kiện: ")
                    .append(String.join(", ", advice.relaxed()))
                    .append(". Phải nói rõ điều này với khách.\n");
        }
        out.append("Tìm được ").append(advice.total()).append(" sản phẩm, xếp theo độ phù hợp, liệt kê ")
                .append(advice.items().size()).append(":\n");
        advice.items().forEach(c -> out.append("- ").append(c.name()).append(": ").append(money(c.price()))
                .append(c.inStock() ? " (còn hàng)" : " (hết hàng)").append(specs(c, intent)).append('\n'));
        return out.toString().trim();
    }

    private static String conditions(ChatIntent intent) {
        List<String> parts = new ArrayList<>();
        if (intent.category() != null) {
            parts.add(CATEGORY_NAMES.getOrDefault(intent.category(), intent.category()));
        }
        if (!intent.brands().isEmpty()) {
            parts.add("hãng " + String.join(", ", intent.brands()));
        }
        if (!intent.models().isEmpty()) {
            parts.add("dòng " + String.join(", ", intent.models()));
        }
        if (intent.need() != null) {
            parts.add(intent.need().label());
        }
        intent.features().forEach(f -> parts.add(f.label()));
        if (intent.minPrice() != null) {
            parts.add("từ " + money(intent.minPrice()));
        }
        if (intent.maxPrice() != null) {
            parts.add("tối đa " + money(intent.maxPrice()));
        }
        return String.join("; ", parts);
    }

    private static String specs(Candidate c, ChatIntent intent) {
        List<String> parts = c.specs().entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, String>>comparingInt(e -> rank(e.getKey()))
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_SPECS)
                .map(e -> e.getKey().trim() + ": " + e.getValue())
                .collect(Collectors.toCollection(ArrayList::new));
        boolean graphicsMatters = ChatIntent.COMPUTER.equals(c.category())
                && (intent.need() == ChatIntent.Need.GRAPHICS || intent.need() == ChatIntent.Need.GAMING);
        if (graphicsMatters && c.specs().keySet().stream().noneMatch(k -> rank(k) == KEY_SPECS.indexOf("card do hoa"))) {
            parts.add("Card đồ hoạ: chưa có thông tin");
        }
        return parts.isEmpty() ? "" : " | " + String.join("; ", parts);
    }

    private static int rank(String key) {
        String plain = SlugUtil.unaccent(key).trim();
        for (int i = 0; i < KEY_SPECS.size(); i++) {
            if (plain.startsWith(KEY_SPECS.get(i))) {
                return i;
            }
        }
        return KEY_SPECS.size();
    }

    private static String money(long vnd) {
        return String.format("%,d", vnd).replace(',', '.') + "₫";
    }
}
