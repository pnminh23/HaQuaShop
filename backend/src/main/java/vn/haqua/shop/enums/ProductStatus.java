package vn.haqua.shop.enums;

public enum ProductStatus {
    DRAFT, ACTIVE, OUT_OF_STOCK, HIDDEN;

    public boolean isVisible() {
        return this == ACTIVE || this == OUT_OF_STOCK;

    }
}
