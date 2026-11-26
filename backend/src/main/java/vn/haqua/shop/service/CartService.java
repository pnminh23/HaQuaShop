package vn.haqua.shop.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.CartDtos.*;
import vn.haqua.shop.dto.CartKey;
import vn.haqua.shop.entity.Cart;
import vn.haqua.shop.entity.CartItem;
import vn.haqua.shop.entity.Coupon;
import vn.haqua.shop.entity.ProductVariant;
import vn.haqua.shop.enums.ShippingMethod;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.CartRepository;
import vn.haqua.shop.repository.ProductVariantRepository;
import vn.haqua.shop.repository.UserRepository;

/** UC06 – Quản lý giỏ hàng, UC07 – áp mã giảm giá. Giá luôn lấy từ CSDL. */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final UserRepository users;
    private final CouponService couponService;
    private final ShippingFeeCalculator shipping;

    // ------------------------------------------------------------ đọc

    @Transactional
    public CartDto get(CartKey key) {
        Cart cart = find(key, false);
        return cart == null ? emptyCart() : toDto(cart, null);
    }

    // ------------------------------------------------------------ ghi

    @Transactional
    public CartDto addItem(CartKey key, AddItemRequest req) {
        Cart cart = find(key, true);
        ProductVariant v = variants.findById(req.variantId())
                .filter(x -> x.getProduct().getStatus().isVisible())
                .orElseThrow(() -> ApiException.notFound("Sản phẩm không tồn tại"));
        CartItem item = cart.find(v.getId());
        int newQty = (item == null ? 0 : item.getQty()) + req.qty();
        assertStock(v, newQty);
        if (item == null) {
            cart.getItems().add(new CartItem(cart, v, newQty));
        } else {
            item.setQty(newQty);
            item.setSavedForLater(false);
        }
        cart.touch();
        carts.saveAndFlush(cart); // để dòng mới có id trả về cho frontend
        return toDto(cart, null);
    }

    /** Đánh dấu giỏ được tạo từ khung chat để ghi kênh CHATBOT khi đặt hàng. */
    @Transactional
    public CartDto addItemFromChatbot(CartKey key, AddItemRequest req) {
        CartDto dto = addItem(key, req);
        find(key, true).setFromChatbot(true);
        return dto;
    }

    @Transactional(readOnly = true)
    public boolean isFromChatbot(CartKey key) {
        Cart cart = find(key, false);
        return cart != null && cart.isFromChatbot();
    }

    @Transactional
    public CartDto updateItem(CartKey key, Long itemId, UpdateItemRequest req) {
        Cart cart = find(key, true);
        CartItem item = item(cart, itemId);
        if (req.qty() != null) {
            if (req.qty() <= 0) {
                cart.getItems().remove(item);
                cart.touch();
                return toDto(cart, null);
            }
            assertStock(item.getVariant(), req.qty());
            item.setQty(req.qty());
        }
        if (req.selected() != null) {
            item.setSelected(req.selected());
        }
        if (req.savedForLater() != null) {
            item.setSavedForLater(req.savedForLater());
            if (req.savedForLater()) {
                item.setSelected(false);
            }
        }
        cart.touch();
        return toDto(cart, null);
    }

    @Transactional
    public CartDto removeItem(CartKey key, Long itemId) {
        Cart cart = find(key, true);
        cart.getItems().remove(item(cart, itemId));
        cart.touch();
        return toDto(cart, null);
    }

    @Transactional
    public CartDto removeSelected(CartKey key) {
        Cart cart = find(key, true);
        cart.getItems().removeIf(CartItem::isSelected);
        cart.touch();
        return toDto(cart, null);
    }

    @Transactional
    public CartDto applyCoupon(CartKey key, String code) {
        Cart cart = find(key, true);
        Coupon c = couponService.validate(code, selectedSubtotal(cart));
        cart.setCoupon(c);
        return toDto(cart, null);
    }

    @Transactional
    public CartDto removeCoupon(CartKey key) {
        Cart cart = find(key, true);
        cart.setCoupon(null);
        return toDto(cart, null);
    }

    /** Gộp giỏ khách vãng lai vào giỏ tài khoản khi đăng ký/đăng nhập. */
    @Transactional
    public void mergeGuestCart(String guestToken, Long userId) {
        if (guestToken == null || guestToken.isBlank()) {
            return;
        }
        carts.findBySessionToken(guestToken).filter(g -> g.getUser() == null).ifPresent(guest -> {
            Cart mine = carts.findByUserId(userId).orElseGet(() -> newCart(userId, null));
            for (CartItem gi : List.copyOf(guest.getItems())) {
                CartItem existing = mine.find(gi.getVariant().getId());
                if (existing == null) {
                    mine.getItems().add(new CartItem(mine, gi.getVariant(), gi.getQty()));
                } else {
                    existing.setQty(Math.min(existing.getQty() + gi.getQty(), Math.max(gi.getVariant().getStock(), 1)));
                }
            }
            carts.delete(guest);
        });
    }

    // ------------------------------------------------------------ dùng bởi
    // checkout

    /** Các dòng đang được chọn để thanh toán. */
    @Transactional(readOnly = true)
    public List<CartItem> selectedItems(CartKey key) {
        Cart cart = find(key, false);
        return cart == null ? List.of()
                : cart.getItems().stream().filter(i -> i.isSelected() && !i.isSavedForLater()).toList();
    }

    @Transactional(readOnly = true)
    public Coupon couponOf(CartKey key) {
        Cart cart = find(key, false);
        return cart == null ? null : cart.getCoupon();
    }

    /** Sau khi đặt hàng: bỏ các dòng đã mua và mã giảm giá. */
    @Transactional
    public void clearPurchased(CartKey key) {
        Cart cart = find(key, false);
        if (cart != null) {
            cart.getItems().removeIf(i -> i.isSelected() && !i.isSavedForLater());
            cart.setCoupon(null);
            cart.touch();
        }
    }

    // ------------------------------------------------------------ nội bộ

    private Cart find(CartKey key, boolean createIfMissing) {
        if (key.userId() != null) {
            return carts.findByUserId(key.userId())
                    .orElseGet(() -> createIfMissing ? newCart(key.userId(), null) : null);
        }
        if (key.token() != null && !key.token().isBlank()) {
            var existing = carts.findBySessionToken(key.token());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return createIfMissing ? newCart(null, key.token() == null || key.token().isBlank()
                ? UUID.randomUUID().toString()
                : key.token()) : null;
    }

    private Cart newCart(Long userId, String token) {
        Cart c = new Cart();
        if (userId != null) {
            c.setUser(users.getReferenceById(userId));
        } else {
            c.setSessionToken(token);
        }
        return carts.save(c);
    }

    private CartItem item(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Không thấy dòng trong giỏ"));
    }

    private void assertStock(ProductVariant v, int qty) {
        if (qty > v.getStock()) {
            throw new ApiException(HttpStatus.CONFLICT, "Chỉ còn " + v.getStock() + " sản phẩm",
                    java.util.Map.of("available", v.getStock(), "variantId", v.getId()));
        }
    }

    private long selectedSubtotal(Cart cart) {
        return cart.getItems().stream().filter(i -> i.isSelected() && !i.isSavedForLater())
                .mapToLong(i -> i.getVariant().getPrice() * i.getQty()).sum();
    }

    private CartDto emptyCart() {
        return new CartDto(null, List.of(), new CartSummary(0, 0, null, 0, 0, 0, 0), 0, null);
    }

    private CartDto toDto(Cart cart, String notice) {
        List<CartItemDto> items = cart.getItems().stream().map(i -> {
            ProductVariant v = i.getVariant();
            return new CartItemDto(i.getId(), v.getId(), v.getProduct().getSlug(), v.getProduct().getName(), v.label(),
                    v.getProduct().mainImageUrl(), v.getPrice(), v.hasDiscount() ? v.getOriginalPrice() : 0, i.getQty(),
                    i.isSelected(), i.isSavedForLater(), v.getStock(), v.getPrice() * i.getQty());
        }).toList();

        var chosen = cart.getItems().stream().filter(i -> i.isSelected() && !i.isSavedForLater()).toList();
        long subtotal = chosen.stream().mapToLong(i -> i.getVariant().getPrice() * i.getQty()).sum();
        long productDiscount = chosen.stream().filter(i -> i.getVariant().hasDiscount())
                .mapToLong(i -> (i.getVariant().getOriginalPrice() - i.getVariant().getPrice()) * i.getQty()).sum();

        long couponDiscount = 0;
        String couponCode = null;
        if (cart.getCoupon() != null) {
            try {
                cart.getCoupon().assertApplicable(subtotal);
                couponDiscount = cart.getCoupon().discountFor(subtotal);
                couponCode = cart.getCoupon().getCode();
            } catch (ApiException ex) {
                cart.setCoupon(null); // không còn đủ điều kiện sau khi đổi giỏ
                notice = "Mã giảm giá đã được gỡ: " + ex.getMessage();
            }
        }
        long ship = shipping.fee(ShippingMethod.STANDARD, subtotal);
        long total = subtotal - couponDiscount + ship;
        int badge = cart.getItems().stream().filter(i -> !i.isSavedForLater()).mapToInt(CartItem::getQty).sum();
        return new CartDto(cart.getSessionToken(), items,
                new CartSummary(subtotal, productDiscount, couponCode, couponDiscount, ship, total, chosen.size()),
                badge, notice);

    }
}
