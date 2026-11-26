package vn.haqua.shop.service.chat;

import java.util.Set;

/** Điều kiện mua hàng hiểu được từ lời khách. Trường null / tập rỗng nghĩa là khách chưa nói. */
public record ChatIntent(String category, Set<String> brands, Long minPrice, Long maxPrice, Need need,
                         Set<Feature> features, Set<String> models, Order order) {

    public static final String COMPUTER = "may-tinh";
    public static final String PHONE = "dien-thoai";
    public static final String AUDIO = "tai-nghe";

    public enum Need {
        GRAPHICS("làm đồ họa / dựng phim"), GAMING("chơi game"), OFFICE("văn phòng, học tập");

        private final String label;

        Need(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Đặc điểm tai nghe khách yêu cầu. */
    public enum Feature {
        NOISE_CANCELLING("chống ồn"), WIRELESS("không dây"), WIRED("có dây"), TRUE_WIRELESS("true wireless"),
        OVER_EAR("choàng đầu"), IN_EAR("nhét tai"), GAMING("gaming");

        private final String label;

        Feature(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Order { CHEAPEST, PREMIUM }

    static final ChatIntent NONE = new ChatIntent(null, Set.of(), null, null, null, Set.of(), Set.of(), null);

    public boolean isEmpty() {
        return equals(NONE);
    }

    public boolean hasPrice() {
        return minPrice != null || maxPrice != null;
    }

    /**
     * Gộp câu mới vào những gì khách đã nói trước đó. Đổi sang loại sản phẩm khác thì bỏ hết điều kiện cũ;
     * nhắc hãng hoặc dòng máy mới thì dòng máy cũ không còn giá trị.
     */
    ChatIntent then(ChatIntent next) {
        if (next.isEmpty()) {
            return this;
        }
        if (category != null && next.category != null && !category.equals(next.category)) {
            return next;
        }
        boolean newTarget = !next.brands.isEmpty() || !next.models.isEmpty();
        return new ChatIntent(
                next.category != null ? next.category : category,
                newTarget ? next.brands : brands,
                next.hasPrice() ? next.minPrice : minPrice,
                next.hasPrice() ? next.maxPrice : maxPrice,
                next.need != null ? next.need : need,
                next.features.isEmpty() ? features : next.features,
                newTarget ? next.models : models,
                next.order != null ? next.order : order);
    }

    /** Khách chỉ nói nhu cầu hoặc đặc điểm thì suy ra loại sản phẩm. */
    ChatIntent withImpliedCategory() {
        String implied = category;
        if (implied == null && !features.isEmpty()) {
            implied = AUDIO;
        } else if (implied == null && need != null) {
            implied = COMPUTER;
        }
        return new ChatIntent(implied, brands, minPrice, maxPrice, need, features, models, order);
    }
}
