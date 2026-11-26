package vn.haqua.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import vn.haqua.shop.config.AppProperties;
import vn.haqua.shop.enums.ShippingMethod;

/** Phí vận chuyển: tiêu chuẩn miễn phí từ ngưỡng, nhanh 2h cố định, nhận tại cửa hàng = 0. */
@Component
@RequiredArgsConstructor
public class ShippingFeeCalculator {

    private final AppProperties props;

    public long fee(ShippingMethod method, long subtotal) {
        return switch (method) {
            case STANDARD -> subtotal >= props.shop().freeShipThreshold() || subtotal == 0 ? 0 : props.shop().standardShipFee();
            case EXPRESS_2H -> props.shop().expressShipFee();
            case STORE_PICKUP -> 0;
        };

    }
}
