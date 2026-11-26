package vn.haqua.shop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import vn.haqua.shop.enums.Role;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Pattern(regexp = "^0\\d{9}$", message = "SĐT gồm 10 số, bắt đầu bằng 0") String phone,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải có cả chữ và số") String password,
            String guestCartToken) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password, String guestCartToken) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds,
                                Long userId, String fullName, Role role) {
    }
}
