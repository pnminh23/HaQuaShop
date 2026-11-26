package vn.haqua.shop.dto;

/** Định danh giỏ: userId nếu đã đăng nhập, ngược lại token phiên của khách vãng lai. */
public record CartKey(Long userId, String token) {
}
