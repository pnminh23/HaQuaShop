package vn.haqua.shop.enums;

public enum PaymentMethod {
    COD, QR, EWALLET, CARD, INSTALLMENT;

    public boolean isOnline() {
        return this != COD;

    }
}
