package vn.haqua.shop.service.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import vn.haqua.shop.service.chat.ChatIntent.Feature;
import vn.haqua.shop.service.chat.ChatIntent.Need;
import vn.haqua.shop.util.SlugUtil;

/** Chọn sản phẩm để đưa cho trợ lý AI, dựa trên cả cuộc trò chuyện chứ không chỉ câu mới nhất. */
public final class ProductAdvisor {

    public static final int LIMIT = 6;

    public record Candidate(long id, String name, String brand, String category, long price, boolean inStock, int sold,
                            Map<String, String> specs) {
    }

    /**
     * @param total           số sản phẩm khớp (items chỉ là tối đa {@value #LIMIT} cái đầu)
     * @param relaxed         điều kiện phải bỏ vì không sản phẩm nào đáp ứng đủ
     * @param productQuestion câu mới nhất có hỏi về sản phẩm không (để quyết định hiện thẻ sản phẩm)
     */
    public record Advice(ChatIntent intent, List<Candidate> items, int total, List<String> relaxed,
                         boolean productQuestion) {
    }

    private static final List<String> VAGUE_REQUESTS = List.of("tu van", "goi y", "nen mua", "ban chay", "san pham nao");

    private static final Pattern DISCRETE_GPU = Pattern.compile("\\b(rtx|gtx|radeon rx|arc [ab]\\d)");
    private static final Pattern RTX_MODEL = Pattern.compile("\\b(?:rtx|gtx) ?(\\d{4})");
    private static final Pattern APPLE_PRO_CHIP = Pattern.compile("\\bm\\d (pro|max)\\b");

    private ProductAdvisor() {
    }

    private record Rule(String label, Predicate<Candidate> test) {
    }

    public static Advice advise(List<String> userMessages, List<Candidate> catalog) {
        Set<String> brands = new LinkedHashSet<>();
        Set<String> nameWords = new HashSet<>();
        for (Candidate c : catalog) {
            if (c.brand() != null && !c.brand().isBlank()) {
                brands.add(c.brand());
            }
            nameWords.addAll(Arrays.asList(SlugUtil.unaccent(c.name()).split("[^a-z0-9]+")));
        }

        ChatIntent said = ChatIntent.NONE;
        ChatIntent last = ChatIntent.NONE;
        boolean lastIsVague = false;
        for (String message : userMessages) {
            last = ChatIntentParser.parse(message, brands, nameWords);
            lastIsVague = isVague(message);
            said = said.then(last);
        }
        ChatIntent intent = said.withImpliedCategory();
        boolean productQuestion = !last.isEmpty() || lastIsVague;

        if (intent.isEmpty()) {
            List<Candidate> popular = lastIsVague
                    ? catalog.stream().filter(Candidate::inStock).sorted(popularFirst()).toList()
                    : List.of();
            return new Advice(intent, top(popular), popular.size(), List.of(), productQuestion);
        }

        // loại sản phẩm là điều kiện cứng: không bao giờ gợi ý sang loại khác
        List<Candidate> pool = catalog.stream()
                .filter(c -> intent.category() == null || intent.category().equals(c.category())).toList();

        List<Rule> rules = rules(intent);
        List<Candidate> matched = filter(pool, rules, 0);
        int dropped = 0;
        if (matched.isEmpty()) {
            // thử bỏ ít điều kiện nhất; cùng số lượng thì ưu tiên bỏ điều kiện đứng trước (giá trước, nhu cầu sau cùng)
            for (int mask : IntStream.range(1, 1 << rules.size()).boxed()
                    .sorted(Comparator.<Integer>comparingInt(Integer::bitCount).thenComparingInt(i -> i)).toList()) {
                matched = filter(pool, rules, mask);
                if (!matched.isEmpty()) {
                    dropped = mask;
                    break;
                }
            }
        }
        List<String> relaxed = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            if ((dropped & (1 << i)) != 0) {
                relaxed.add(rules.get(i).label());
            }
        }

        List<Candidate> sorted = matched.stream()
                .sorted(Comparator.comparing((Candidate c) -> !c.inStock()).thenComparing(order(intent, relaxed)))
                .toList();
        return new Advice(intent, top(sorted), sorted.size(), relaxed, productQuestion);
    }

    private static boolean isVague(String message) {
        String words = " " + SlugUtil.unaccent(message).replaceAll("[^a-z0-9]+", " ").trim() + " ";
        return VAGUE_REQUESTS.stream().anyMatch(p -> words.contains(" " + p + " "));
    }

    private static List<Candidate> top(List<Candidate> list) {
        return list.stream().limit(LIMIT).toList();
    }

    private static List<Candidate> filter(List<Candidate> pool, List<Rule> rules, int skipMask) {
        return pool.stream()
                .filter(c -> IntStream.range(0, rules.size())
                        .allMatch(i -> (skipMask & (1 << i)) != 0 || rules.get(i).test().test(c)))
                .toList();
    }

    /** Thứ tự trong danh sách cũng là thứ tự được bỏ khi không có gì khớp. */
    private static List<Rule> rules(ChatIntent intent) {
        List<Rule> rules = new ArrayList<>();
        if (intent.hasPrice()) {
            rules.add(new Rule("mức giá", c -> (intent.minPrice() == null || c.price() >= intent.minPrice())
                    && (intent.maxPrice() == null || c.price() <= intent.maxPrice())));
        }
        if (!intent.features().isEmpty() && ChatIntent.AUDIO.equals(intent.category())) {
            rules.add(new Rule("tính năng", c -> intent.features().stream().allMatch(f -> hasFeature(c, f))));
        }
        if (!intent.models().isEmpty()) {
            rules.add(new Rule("dòng máy", c -> {
                Set<String> words = Set.of(distinct(SlugUtil.unaccent(c.name()).split("[^a-z0-9]+")));
                return intent.models().stream().anyMatch(words::contains);
            }));
        }
        if (!intent.brands().isEmpty()) {
            rules.add(new Rule("hãng", c -> intent.brands().stream().anyMatch(b -> b.equalsIgnoreCase(c.brand()))));
        }
        if (ChatIntent.COMPUTER.equals(intent.category())
                && (intent.need() == Need.GRAPHICS || intent.need() == Need.GAMING)) {
            rules.add(new Rule("nhu cầu", ProductAdvisor::strongGraphics));
        }
        return rules;
    }

    private static String[] distinct(String[] words) {
        return Arrays.stream(words).distinct().toArray(String[]::new);
    }

    private static Comparator<Candidate> order(ChatIntent intent, List<String> relaxed) {
        Comparator<Candidate> cheap = Comparator.comparingLong(Candidate::price);
        if (relaxed.contains("mức giá")) {
            return Comparator.<Candidate>comparingLong(c -> distance(c.price(), intent)).thenComparing(cheap);
        }
        if (intent.order() == ChatIntent.Order.CHEAPEST) {
            return cheap;
        }
        if (intent.order() == ChatIntent.Order.PREMIUM) {
            return cheap.reversed();
        }
        boolean needsGraphics = intent.need() == Need.GRAPHICS || intent.need() == Need.GAMING;
        if (needsGraphics) {
            // có ngân sách: máy mạnh nhất trong tầm; chưa có: bắt đầu từ máy dễ mua nhất
            return intent.maxPrice() == null ? cheap
                    : Comparator.<Candidate>comparingInt(ProductAdvisor::gpuTier).reversed().thenComparing(cheap.reversed());
        }
        if (intent.need() == Need.OFFICE) {
            return Comparator.comparing(ProductAdvisor::strongGraphics).thenComparing(cheap);
        }
        return popularFirst().thenComparing(intent.maxPrice() == null ? cheap : cheap.reversed());
    }

    private static Comparator<Candidate> popularFirst() {
        return Comparator.<Candidate>comparingInt(Candidate::sold).reversed();
    }

    private static long distance(long price, ChatIntent intent) {
        if (intent.maxPrice() != null && price > intent.maxPrice()) {
            return price - intent.maxPrice();
        }
        return intent.minPrice() != null && price < intent.minPrice() ? intent.minPrice() - price : 0;
    }

    private static String gpuSpec(Candidate c) {
        return c.specs().entrySet().stream()
                .filter(e -> SlugUtil.unaccent(e.getKey()).trim().startsWith("card do hoa"))
                .map(e -> SlugUtil.unaccent(e.getValue())).findFirst().orElse(null);
    }

    private static boolean strongGraphics(Candidate c) {
        String gpu = gpuSpec(c);
        String name = SlugUtil.unaccent(c.name());
        if ((gpu != null && DISCRETE_GPU.matcher(gpu).find()) || DISCRETE_GPU.matcher(name).find()) {
            return true;
        }
        if (appleProChip(c)) {
            return true;
        }
        // dữ liệu nhập về hay thiếu dòng card: máy gaming không ghi card vẫn được tính, trợ lý sẽ nói là chưa rõ card
        return gpu == null && name.contains("gaming");
    }

    private static boolean appleProChip(Candidate c) {
        return "apple".equalsIgnoreCase(c.brand())
                && APPLE_PRO_CHIP.matcher(SlugUtil.unaccent(c.name() + " " + String.join(" ", c.specs().values()))).find();
    }

    /** Xếp hạng thô sức mạnh đồ họa, chỉ để sắp thứ tự gợi ý. */
    private static int gpuTier(Candidate c) {
        Matcher m = RTX_MODEL.matcher(SlugUtil.unaccent(c.name() + " " + gpuSpec(c)));
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return appleProChip(c) ? 5060 : 0;
    }

    private static boolean hasFeature(Candidate c, Feature feature) {
        String name = SlugUtil.unaccent(c.name());
        String all = name + " " + SlugUtil.unaccent(String.join(" ", c.specs().values()));
        return switch (feature) {
            case NOISE_CANCELLING -> all.contains("chong on") || all.contains("khu tieng on");
            case WIRELESS -> !name.contains("co day") && (all.contains("bluetooth") || all.contains("khong day"));
            case WIRED -> name.contains("co day");
            case TRUE_WIRELESS -> all.contains("true wireless");
            case OVER_EAR -> name.contains("choang dau") || name.contains("chup tai");
            case IN_EAR -> name.contains("nhet tai") || name.contains("in-ear");
            case GAMING -> name.contains("gaming");
        };
    }
}
