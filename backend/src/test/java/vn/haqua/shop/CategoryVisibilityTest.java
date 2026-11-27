package vn.haqua.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import vn.haqua.shop.dto.CartDtos.AddItemRequest;
import vn.haqua.shop.dto.CartKey;
import vn.haqua.shop.dto.CatalogDtos.CategoryDto;
import vn.haqua.shop.dto.CatalogDtos.ProductCardDto;
import vn.haqua.shop.dto.ProductFilter;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.service.BrandService;
import vn.haqua.shop.service.CartService;
import vn.haqua.shop.service.CategoryService;
import vn.haqua.shop.service.KnowledgeRetriever;
import vn.haqua.shop.service.ProductService;

/** Ẩn một danh mục thì khách không được thấy danh mục lẫn sản phẩm của nó ở bất kỳ đâu. Chạy trên H2 riêng. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:category-visibility;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "haqua.seed.enabled=true", "haqua.import.dir=",
        "haqua.upload-dir=target/test-uploads"})
class CategoryVisibilityTest {

    @Autowired CategoryService categories;
    @Autowired ProductService products;
    @Autowired BrandService brands;
    @Autowired CartService carts;
    @Autowired KnowledgeRetriever retriever;

    private Long computers;
    private ProductCardDto laptop;

    @BeforeEach
    void pickComputer() {
        computers = categories.listAll().stream().filter(c -> c.slug().equals("may-tinh")).findFirst().orElseThrow().id();
        categories.setHidden(computers, false);
        brands.detectComputerBrands();
        laptop = search(null).stream().filter(p -> p.categorySlug().equals("may-tinh") && p.defaultVariantId() != null)
                .findFirst().orElseThrow();
    }

    @AfterEach
    void showAgain() {
        categories.setHidden(computers, false);
    }

    private List<ProductCardDto> search(String q) {
        return products.search(new ProductFilter(q, null, null, null, null, null, null, "popular", null), 0, 48)
                .page().content();
    }

    private static List<String> categoriesOf(List<ProductCardDto> cards) {
        return cards.stream().map(ProductCardDto::categorySlug).distinct().toList();
    }

    @Test
    void shopCategoryListHidesItButAdminStillSeesIt() {
        categories.setHidden(computers, true);

        assertThat(categories.list()).extracting(CategoryDto::slug).doesNotContain("may-tinh").contains("dien-thoai");
        assertThat(categories.listAll()).filteredOn(c -> c.id().equals(computers)).extracting(CategoryDto::hidden)
                .containsExactly(true);
    }

    @Test
    void listFacetsSuggestAndFeaturedDropItsProducts() {
        categories.setHidden(computers, true);

        assertThat(categoriesOf(search(null))).isNotEmpty().doesNotContain("may-tinh");
        assertThat(products.search(new ProductFilter(null, List.of("may-tinh"), null, null, null, null, null, null, null), 0, 48)
                .page().content()).isEmpty();
        assertThat(products.search(new ProductFilter(null, null, null, null, null, null, null, null, null), 0, 48)
                .categoryFacets()).doesNotContainKey("may-tinh");
        assertThat(products.suggest(laptop.name())).isEmpty();
        assertThat(products.featured(24)).extracting(ProductCardDto::categorySlug).doesNotContain("may-tinh");
    }

    @Test
    void detailPageIsNotFound() {
        categories.setHidden(computers, true);

        assertThatThrownBy(() -> products.detail(laptop.slug())).isInstanceOf(ApiException.class);
    }

    @Test
    void chatbotAndComputerBrandMenuForgetIt() {
        assertThat(brands.computerMenu()).isNotEmpty();
        categories.setHidden(computers, true);

        assertThat(brands.computerMenu()).isEmpty();
        assertThat(retriever.retrieve(List.of("tư vấn laptop")).products()).isEmpty();
        assertThat(retriever.retrieve(List.of("tư vấn laptop")).context()).doesNotContain(laptop.name());
    }

    @Test
    void cannotBeAddedToCart() {
        categories.setHidden(computers, true);

        assertThatThrownBy(() -> carts.addItem(new CartKey(null, "guest-hidden-add"),
                new AddItemRequest(laptop.defaultVariantId(), 1))).isInstanceOf(ApiException.class);
    }

    @Test
    void itemAlreadyInCartIsSetAsideAndCannotBeBoughtUntilShownAgain() {
        CartKey key = new CartKey(null, "guest-hidden-existing");
        carts.addItem(key, new AddItemRequest(laptop.defaultVariantId(), 1));

        categories.setHidden(computers, true);

        assertThat(carts.get(key).items()).isEmpty();
        assertThat(carts.get(key).summary().total()).isZero();
        assertThat(carts.get(key).itemCount()).isZero();
        assertThat(carts.get(key).notice()).contains("tạm ngừng bán");
        assertThat(carts.selectedItems(key)).isEmpty();
        carts.removeSelected(key); // xóa mục đã chọn không được xóa mất dòng đang bị ẩn

        categories.setHidden(computers, false);

        assertThat(carts.get(key).items()).hasSize(1);
    }

    @Test
    void showingAgainRestoresEverything() {
        int before = search(null).size();
        categories.setHidden(computers, true);
        categories.setHidden(computers, false);

        assertThat(search(null)).hasSize(before);
        assertThat(products.detail(laptop.slug()).name()).isEqualTo(laptop.name());
    }
}
