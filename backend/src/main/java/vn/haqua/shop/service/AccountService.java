package vn.haqua.shop.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.AccountDtos.AddressDto;
import vn.haqua.shop.dto.AccountDtos.ChangePasswordRequest;
import vn.haqua.shop.dto.AccountDtos.ProfileResponse;
import vn.haqua.shop.dto.AccountDtos.UpdateProfileRequest;
import vn.haqua.shop.entity.Address;
import vn.haqua.shop.entity.User;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.AddressRepository;
import vn.haqua.shop.repository.UserRepository;

/** UC22 – Quản lý tài khoản cá nhân. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository users;
    private final AddressRepository addresses;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        User u = get(userId);
        List<AddressDto> list = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
                .map(AccountService::toDto).toList();
        return new ProfileResponse(u.getId(), u.getEmail(), u.getPhone(), u.getFullName(), u.getRole(),
                u.getGreenPoints(), list);
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest req) {
        User u = get(userId);
        if (!u.getEmail().equalsIgnoreCase(req.email())
                && users.findByEmailIgnoreCaseOrPhone(req.email(), "").filter(o -> !o.getId().equals(userId)).isPresent()) {
            throw ApiException.conflict("Email đã thuộc tài khoản khác");
        }
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        return profile(userId);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User u = get(userId);
        if (!encoder.matches(req.oldPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu cũ không đúng");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
    }

    @Transactional
    public AddressDto saveAddress(Long userId, Long addressId, AddressDto req) {
        User u = get(userId);
        Address a = addressId == null ? new Address()
                : addresses.findByIdAndUserId(addressId, userId).orElseThrow(() -> ApiException.notFound("Không thấy địa chỉ"));
        a.setUser(u);
        a.setReceiverName(req.receiverName());
        a.setPhone(req.phone());
        a.setProvince(req.province());
        a.setWard(req.ward());
        a.setDetail(req.detail());
        if (req.defaultAddress()) {
            addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).forEach(o -> o.setDefaultAddress(false));
        }
        a.setDefaultAddress(req.defaultAddress());
        return toDto(addresses.save(a));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        addresses.delete(addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> ApiException.notFound("Không thấy địa chỉ")));
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy người dùng"));
    }

    private static AddressDto toDto(Address a) {
        return new AddressDto(a.getId(), a.getReceiverName(), a.getPhone(), a.getProvince(), a.getWard(),
                a.getDetail(), a.isDefaultAddress());

    }
}
