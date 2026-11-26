package vn.haqua.shop.service.chat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.haqua.shop.service.chat.ChatIntent.Feature;
import vn.haqua.shop.service.chat.ChatIntent.Need;
import vn.haqua.shop.util.SlugUtil;

/** Đọc một câu của khách thành {@link ChatIntent}. Mọi so khớp đều theo nguyên từ, trên chữ đã bỏ dấu. */
final class ChatIntentParser {

    private ChatIntentParser() {
    }

    /** Cách gọi loại sản phẩm; loại nào được nhắc trước trong câu thì thắng ("tai nghe cho điện thoại"). */
    private static final Map<String, List<String>> CATEGORIES = Map.of(
            ChatIntent.COMPUTER, List.of("laptop", "lap top", "may tinh", "macbook", "notebook"),
            ChatIntent.PHONE, List.of("dien thoai", "smartphone", "iphone"),
            ChatIntent.AUDIO, List.of("tai nghe", "tainghe", "headphone", "headset", "earbud", "earbuds", "airpods"));

    /** Dòng sản phẩm ngầm chỉ hãng. */
    private static final Map<String, String> BRAND_ALIASES = Map.of("macbook", "apple", "iphone", "apple", "airpods", "apple");

    private static final Map<Need, List<String>> NEEDS = new LinkedHashMap<>();
    private static final Map<Feature, List<String>> FEATURES = new LinkedHashMap<>();

    static {
        NEEDS.put(Need.GRAPHICS, List.of("do hoa", "thiet ke", "render", "dung phim", "dung video", "edit video",
                "photoshop", "premiere", "illustrator", "autocad", "3d", "card roi"));
        NEEDS.put(Need.GAMING, List.of("gaming", "choi game", "game"));
        NEEDS.put(Need.OFFICE, List.of("van phong", "hoc tap", "sinh vien", "hoc sinh"));
        FEATURES.put(Feature.NOISE_CANCELLING, List.of("chong on", "khu tieng on", "anc"));
        FEATURES.put(Feature.TRUE_WIRELESS, List.of("true wireless", "tws"));
        FEATURES.put(Feature.WIRELESS, List.of("khong day", "bluetooth"));
        FEATURES.put(Feature.WIRED, List.of("co day"));
        FEATURES.put(Feature.OVER_EAR, List.of("choang dau", "chup tai"));
        FEATURES.put(Feature.IN_EAR, List.of("nhet tai", "in ear"));
    }

    /** Từ chung chung trong tên sản phẩm, không dùng để nhận ra dòng máy. */
    private static final Set<String> GENERIC_NAME_WORDS = Set.of("laptop", "may", "tinh", "dien", "thoai", "tai",
            "nghe", "bluetooth", "gaming", "game", "khong", "day", "choang", "dau", "chong", "nhet", "the", "thao",
            "phone", "pro", "max", "plus", "ultra", "core", "ram", "new", "true", "wireless", "van", "phong", "gia",
            "shop", "ban", "cho", "con", "hang", "mua", "xem", "loai", "mau");

    private static final String NUM = "(\\d{1,3}(?:\\.\\d)?)";
    private static final String UNIT = " ?(?:trieu|tr|cu)\\b";
    private static final Pattern RANGE = Pattern.compile("\\b" + NUM + "(?:" + UNIT + ")? ?(?:-|den|toi) ?" + NUM + UNIT);
    private static final Pattern UNDER = Pattern.compile(
            "(?:\\bduoi|\\bkhong qua|\\btoi da|\\bmax|\\bre hon|\\bnho hon|\\bit hon|<=?) ?" + NUM + UNIT);
    private static final Pattern UNDER_AFTER = Pattern.compile("\\b" + NUM + UNIT + " (?:tro xuong|do lai)");
    private static final Pattern OVER = Pattern.compile("(?:\\btren|\\bhon|\\btu|>=?) ?" + NUM + UNIT);
    private static final Pattern OVER_AFTER = Pattern.compile("\\b" + NUM + UNIT + " tro len");
    private static final Pattern AROUND = Pattern.compile("\\b(?:tam|khoang|co|quanh|gan) (?:gia )?" + NUM + UNIT);
    private static final Pattern BARE = Pattern.compile("\\b" + NUM + UNIT);

    /**
     * @param brands    tên các hãng đang có hàng
     * @param nameWords các từ (đã bỏ dấu) xuất hiện trong tên sản phẩm đang bán
     */
    static ChatIntent parse(String message, Set<String> brands, Set<String> nameWords) {
        String raw = message == null ? "" : message.toLowerCase(Locale.ROOT);
        String plain = SlugUtil.unaccent(raw).replace("may tinh bang", " ");
        String words = " " + plain.replaceAll("[^a-z0-9]+", " ").trim() + " ";

        String category = category(words);

        Set<String> found = new LinkedHashSet<>();
        for (String brand : brands) {
            if (has(words, SlugUtil.unaccent(brand).replaceAll("[^a-z0-9]+", " ").trim())) {
                found.add(brand);
            }
        }
        BRAND_ALIASES.forEach((alias, brand) -> {
            if (has(words, alias)) {
                brands.stream().filter(b -> b.equalsIgnoreCase(brand)).forEach(found::add);
            }
        });

        Need need = NEEDS.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(p -> has(words, p)))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        Set<Feature> features = EnumSet.noneOf(Feature.class);
        FEATURES.forEach((feature, phrases) -> {
            if (phrases.stream().anyMatch(p -> has(words, p))) {
                features.add(feature);
            }
        });
        if (ChatIntent.AUDIO.equals(category) && need == Need.GAMING) {
            features.add(Feature.GAMING); // với tai nghe, "gaming" là kiểu sản phẩm chứ không phải nhu cầu cấu hình
            need = null;
        }

        ChatIntent.Order order = has(words, "re nhat") ? ChatIntent.Order.CHEAPEST
                : has(words, "manh nhat") || has(words, "cao cap nhat") || has(words, "dat nhat") ? ChatIntent.Order.PREMIUM
                : null;

        Long[] price = price(plain);
        return new ChatIntent(category, found, price[0], price[1], need, features,
                models(raw, brands, nameWords), order);
    }

    private static boolean has(String paddedWords, String phrase) {
        return !phrase.isEmpty() && paddedWords.contains(" " + phrase + " ");
    }

    private static String category(String words) {
        String best = null;
        int bestAt = Integer.MAX_VALUE;
        for (var entry : CATEGORIES.entrySet()) {
            for (String phrase : entry.getValue()) {
                int at = words.indexOf(" " + phrase + " ");
                if (at >= 0 && at < bestAt) {
                    best = entry.getKey();
                    bestAt = at;
                }
            }
        }
        return best;
    }

    /** Dòng máy (vivobook, macbook, reno…): từ khách gõ không dấu và có trong tên sản phẩm. */
    private static Set<String> models(String raw, Set<String> brands, Set<String> nameWords) {
        Set<String> brandWords = new LinkedHashSet<>();
        brands.forEach(b -> brandWords.addAll(Arrays.asList(SlugUtil.unaccent(b).split("[^a-z0-9]+"))));
        Set<String> out = new LinkedHashSet<>();
        for (String word : raw.split("[^\\p{L}\\p{N}]+")) {
            if (word.length() >= 3 && word.equals(SlugUtil.unaccent(word)) && !word.chars().allMatch(Character::isDigit)
                    && !GENERIC_NAME_WORDS.contains(word) && !brandWords.contains(word) && nameWords.contains(word)) {
                out.add(word);
            }
        }
        return out;
    }

    /** @return {giá thấp nhất, giá cao nhất} tính bằng đồng; chỉ nhận số có đơn vị tiền ("16gb" không phải giá). */
    private static Long[] price(String plain) {
        String text = plain
                .replaceAll("\\b(\\d{1,3})\\.(\\d{3})\\.(\\d{3})\\b", "$1.$2$3 dongtrieu")
                .replaceAll("(\\d+)\\.(\\d)\\d* dongtrieu", "$1.$2 trieu")
                .replaceAll("(\\d),(\\d)", "$1.$2")
                .replaceAll("\\b(\\d{1,3})tr(\\d)\\b", "$1.$2tr")
                .replaceAll("\\s+", " ");
        Matcher m;
        if ((m = RANGE.matcher(text)).find()) {
            return new Long[] {vnd(m.group(1), 1), vnd(m.group(2), 1)};
        }
        if ((m = UNDER.matcher(text)).find() || (m = UNDER_AFTER.matcher(text)).find()) {
            return new Long[] {null, vnd(m.group(1), 1)};
        }
        if ((m = OVER_AFTER.matcher(text)).find() || (m = OVER.matcher(text)).find()) {
            return new Long[] {vnd(m.group(1), 1), null};
        }
        if ((m = AROUND.matcher(text)).find()) {
            return new Long[] {vnd(m.group(1), 0.8), vnd(m.group(1), 1.15)};
        }
        if ((m = BARE.matcher(text)).find()) {
            return new Long[] {null, vnd(m.group(1), 1)};
        }
        return new Long[] {null, null};
    }

    private static long vnd(String millions, double factor) {
        return Math.round(Double.parseDouble(millions) * factor * 1_000_000);
    }
}
