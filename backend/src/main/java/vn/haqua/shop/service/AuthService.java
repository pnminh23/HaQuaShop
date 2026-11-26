package vn.haqua.shop.service;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.AuthDtos.*;
import vn.haqua.shop.entity.User;
import vn.haqua.shop.enums.Role;
import vn.haqua.shop.enums.UserStatus;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.UserRepository;

/** UC01 – Đăng ký, UC02 – Đăng nhập. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CartService carts;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCaseOrPhone(req.email(), req.phone())) {
            throw ApiException.conflict("Email hoặc số điện thoại đã được sử dụng, hãy đăng nhập");
        }
        User u = users.save(new User(req.email().toLowerCase(), req.phone(), encoder.encode(req.password()),
                req.fullName(), Role.CUSTOMER));
        carts.mergeGuestCart(req.guestCartToken(), u.getId());
        return issue(u);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse login(LoginRequest req) {
        User u = users.findByEmailIgnoreCaseOrPhone(req.username(), req.username())
                .orElseThrow(() -> new BadCredentialsException("bad"));
        if (u.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("locked");
        }
        if (u.isLoginLocked()) {
            throw ApiException.forbidden("Đăng nhập tạm khóa, vui lòng thử lại sau");
        }
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            u.setFailedLogins(u.getFailedLogins() + 1);
            if (u.getFailedLogins() >= MAX_FAILED) {
                u.setLockedUntil(Instant.now().plus(LOCK_TIME));
                u.setFailedLogins(0);
            }
            throw new BadCredentialsException("bad");
        }
        u.setFailedLogins(0);
        u.setLockedUntil(null);
        carts.mergeGuestCart(req.guestCartToken(), u.getId());
        return issue(u);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest req) {
        Long id = jwt.parseRefresh(req.refreshToken());
        User u = users.findById(id).orElseThrow(() -> new BadCredentialsException("bad"));
        if (u.getStatus() == UserStatus.LOCKED) {
            throw new DisabledException("locked");
        }
        return issue(u);
    }

    private TokenResponse issue(User u) {
        return new TokenResponse(jwt.accessToken(u), jwt.refreshToken(u), jwt.accessTtlSeconds(),
                u.getId(), u.getFullName(), u.getRole());

    }
}
