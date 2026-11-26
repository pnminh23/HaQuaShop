package vn.haqua.shop.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import vn.haqua.shop.enums.Role;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record ProfileResponse(Long id, String email, String phone, String fullName, Role role,
                                  int greenPoints, List<AddressDto> addresses) {
    }

    public record UpdateProfileRequest(@NotBlank String fullName, @Email @NotBlank String email) {
    }

    public record ChangePasswordRequest(@NotBlank String oldPassword,
                                        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                                                message = "Mật khẩu phải có cả chữ và số") String newPassword) {
    }

    public record AddressDto(Long id, @NotBlank String receiverName,
                             @NotBlank @Pattern(regexp = "^0\\d{9}$", message = "SĐT gồm 10 số, bắt đầu bằng 0") String phone,
                             @NotBlank String province, @NotBlank String ward, @NotBlank String detail,
                             boolean defaultAddress) {
    }
}
