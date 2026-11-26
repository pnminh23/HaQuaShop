package vn.haqua.shop.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lấy người dùng hiện tại (nếu đã đăng nhập) từ JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(Long.valueOf(jwt.getSubject()));
        }
        return Optional.empty();
    }

    public static Long requireId() {
        return id().orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Chưa đăng nhập"));

    }
}
