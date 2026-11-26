package vn.haqua.shop.service.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import vn.haqua.shop.service.chat.ProductAdvisor.Candidate;

class ProductContextTest {

    private static final List<Candidate> CATALOG = List.of(
            new Candidate(1, "MSI Gaming Cyborg 15", "MSI", "may-tinh", 41_990_000, true, 0,
                    Map.of("CPU", "Core i7", "RAM", "16 GB", "Card đồ hoạ", "NVIDIA GeForce RTX 5050 8GB")),
            new Candidate(2, "MSI Gaming Stealth 16", "MSI", "may-tinh", 86_990_000, false, 0, Map.of("RAM", "32 GB")),
            new Candidate(3, "Tai nghe Sony WH-CH730N", "Sony", "tai-nghe", 3_290_000, true, 0, Map.of()));

    private static String contextFor(String... messages) {
        return ProductContext.describe(ProductAdvisor.advise(List.of(messages), CATALOG), CATALOG);
    }

    @Test
    void givesPriceStockAndSpecsForEachProduct() {
        assertThat(contextFor("laptop đồ họa"))
                .contains("MSI Gaming Cyborg 15: 41.990.000₫ (còn hàng)")
                .contains("CPU: Core i7", "RAM: 16 GB", "Card đồ hoạ: NVIDIA GeForce RTX 5050 8GB")
                .contains("MSI Gaming Stealth 16: 86.990.000₫ (hết hàng)")
                .doesNotContain("Sony WH-CH730N");
    }

    @Test
    void marksMissingGraphicsCardInsteadOfLettingAiGuess() {
        assertThat(contextFor("laptop đồ họa")).contains("Card đồ hoạ: chưa có thông tin");
    }

    @Test
    void statesUnderstoodConditionsAndThatListIsOnlyPartOfStock() {
        assertThat(contextFor("laptop đồ họa dưới 90 triệu"))
                .contains("máy tính", "làm đồ họa", "tối đa 90.000.000₫")
                .contains("Shop đang bán: 2 máy tính, 1 tai nghe");
    }

    @Test
    void saysWhichConditionCouldNotBeMet() {
        assertThat(contextFor("laptop dưới 10 triệu"))
                .contains("Không có sản phẩm nào đáp ứng đủ", "mức giá");
    }

    @Test
    void saysWhenShopHasNothingOfThatKind() {
        assertThat(contextFor("điện thoại")).contains("hiện chưa có điện thoại");
    }

    @Test
    void isEmptyForNonProductQuestion() {
        assertThat(contextFor("có trả góp không")).isEmpty();
    }
}
