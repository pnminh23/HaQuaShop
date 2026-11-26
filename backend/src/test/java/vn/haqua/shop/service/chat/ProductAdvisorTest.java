package vn.haqua.shop.service.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import vn.haqua.shop.service.chat.ProductAdvisor.Advice;
import vn.haqua.shop.service.chat.ProductAdvisor.Candidate;

class ProductAdvisorTest {

    private static final String PC = "may-tinh";
    private static final String PHONE = "dien-thoai";
    private static final String AUDIO = "tai-nghe";

    private static Candidate item(long id, String category, String brand, String name, long price, Map<String, String> specs) {
        return new Candidate(id, name, brand, category, price, true, 0, specs);
    }

    private static Map<String, String> gpu(String value) {
        return Map.of("Card đồ hoạ", value, "RAM", "16 GB");
    }

    private static final List<Candidate> CATALOG = List.of(
            item(1, PC, "Asus", "Asus TUF Gaming F16 FX607VJG Core 5 205H", 32_990_000, gpu("NVIDIA GeForce RTX 3050 6GB GDDR6")),
            item(2, PC, "Asus", "Asus Gaming ROG Zephyrus G16 U7 255H", 82_990_000, gpu("NVIDIA GeForce RTX 5070 8GB GDDR7")),
            item(3, PC, "MSI", "MSI Gaming Cyborg 15 i7 13620H", 41_990_000, gpu("NVIDIA GeForce RTX 5050 8GB GDDR7")),
            item(4, PC, "Asus", "Asus Vivobook 14 X1404VA Core 3 100U", 18_490_000, gpu("Intel Graphics")),
            item(5, PC, "Lenovo", "Lenovo IdeaPad Slim 3 14AHP10 R7", 20_490_000, Map.of("RAM", "16 GB")),
            item(6, PC, "Apple", "MacBook Pro 14 M5 Pro 15CPU/16GPU", 79_990_000, Map.of("CPU", "M5 Pro")),
            item(7, AUDIO, "Sony", "Tai nghe Bluetooth choàng đầu chống ồn Sony WH-CH730N", 3_290_000, Map.of("Kiểu kết nối ", "Bluetooth")),
            item(8, AUDIO, "JBL", "Tai nghe có dây choàng đầu JBL Quantum 120", 690_000, Map.of()),
            item(9, AUDIO, "Asus", "Tai nghe có dây choàng đầu Gaming Asus TUF H1 Gen II", 1_190_000, Map.of()),
            item(10, PHONE, "Apple", "iPhone 18 Pro 256GB", 34_990_000, Map.of("RAM", "12 GB")),
            item(11, PHONE, "Samsung", "Samsung Galaxy A37 5G 8GB", 9_990_000, Map.of("RAM", "8 GB")),
            item(12, PHONE, "OPPO", "OPPO Reno16 F 5G 8GB", 15_690_000, Map.of("RAM", "8 GB")));

    private static Advice ask(String... userMessages) {
        return ProductAdvisor.advise(Arrays.asList(userMessages), CATALOG);
    }

    private static List<Long> ids(Advice a) {
        return a.items().stream().map(Candidate::id).toList();
    }

    @Test
    void graphicsLaptopReturnsOnlyCapableComputers() {
        Advice a = ask("tư vấn laptop đồ họa");

        assertThat(ids(a)).containsExactlyInAnyOrder(1L, 2L, 3L, 6L);
        assertThat(a.relaxed()).isEmpty();
        assertThat(a.productQuestion()).isTrue();
    }

    @Test
    void worksWithoutDiacritics() {
        assertThat(ids(ask("tu van laptop do hoa duoi 40tr"))).containsExactly(1L);
    }

    @Test
    void followUpBudgetKeepsEarlierNeed() {
        assertThat(ids(ask("tư vấn laptop đồ họa", "dưới 45 triệu thôi"))).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void switchingCategoryDropsOldConditions() {
        Advice a = ask("laptop đồ họa dưới 45 triệu", "tai nghe chống ồn");

        assertThat(ids(a)).containsExactly(7L);
        assertThat(a.relaxed()).isEmpty();
    }

    @Test
    void brandAndBudgetFilterTogether() {
        assertThat(ids(ask("laptop asus dưới 20 triệu"))).containsExactly(4L);
    }

    @Test
    void newBrandKeepsCategoryAndBudget() {
        assertThat(ids(ask("laptop asus dưới 25 triệu", "còn lenovo thì sao"))).containsExactly(5L);
    }

    @Test
    void understandsBudgetRange() {
        assertThat(ids(ask("laptop từ 30 đến 45 triệu"))).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void understandsAroundBudgetAndSlang() {
        assertThat(ids(ask("điện thoại tầm 10 triệu"))).containsExactly(11L);
        assertThat(ids(ask("laptop 19 củ"))).containsExactly(4L);
    }

    @Test
    void ramSizeIsNotABudget() {
        assertThat(ids(ask("laptop ram 16gb"))).hasSize(6);
    }

    @Test
    void whenNothingFitsBudgetSuggestsClosestInSameCategory() {
        Advice a = ask("laptop đồ họa dưới 20 triệu");

        assertThat(ids(a)).startsWith(1L, 3L).doesNotContain(7L, 8L, 9L, 4L, 5L);
        assertThat(a.relaxed()).containsExactly("mức giá");
    }

    @Test
    void whenBrandHasNoMatchSuggestsOtherBrandsAndSaysSo() {
        Advice a = ask("laptop lenovo đồ họa");

        assertThat(ids(a)).containsExactlyInAnyOrder(1L, 2L, 3L, 6L);
        assertThat(a.relaxed()).containsExactly("hãng");
    }

    @Test
    void productLineNarrowsResultAndIsForgottenWhenBrandChanges() {
        assertThat(ids(ask("macbook giá bao nhiêu"))).containsExactly(6L);
        assertThat(ids(ask("iphone còn hàng không", "samsung thì sao"))).containsExactly(11L);
    }

    @Test
    void tabletIsNotTreatedAsComputer() {
        assertThat(ask("máy tính bảng").intent().category()).isNull();
    }

    @Test
    void policyQuestionAloneBringsNoProducts() {
        Advice a = ask("shop có trả góp không");

        assertThat(a.items()).isEmpty();
        assertThat(a.productQuestion()).isFalse();
    }

    @Test
    void unrelatedFollowUpKeepsContextButShowsNoCards() {
        Advice a = ask("tư vấn laptop đồ họa", "có trả góp không");

        assertThat(a.items()).isNotEmpty();
        assertThat(a.productQuestion()).isFalse();
    }

    @Test
    void vagueRequestForAdviceStillGetsSuggestions() {
        assertThat(ask("tư vấn giúp mình với").items()).isNotEmpty();
    }

    @Test
    void gamingLaptopWithoutGpuSpecIsNotRuledOut() {
        var catalog = List.of(
                item(20, PC, "MSI", "MSI Gaming Stealth 16 AI+ U9 386H", 86_990_000, Map.of()),
                item(21, PC, "Lenovo", "Lenovo IdeaPad Slim 3 R7", 20_490_000, Map.of()));

        assertThat(ids(ProductAdvisor.advise(List.of("laptop đồ họa"), catalog))).containsExactly(20L);
    }
}
