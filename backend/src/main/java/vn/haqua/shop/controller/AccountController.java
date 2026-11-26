package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.AccountDtos.*;
import vn.haqua.shop.service.AccountService;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @GetMapping
    ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.profile(Long.valueOf(jwt.getSubject()));
    }

    @PutMapping
    ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest req) {
        return service.update(Long.valueOf(jwt.getSubject()), req);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest req) {
        service.changePassword(Long.valueOf(jwt.getSubject()), req);
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    AddressDto addAddress(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressDto req) {
        return service.saveAddress(Long.valueOf(jwt.getSubject()), null, req);
    }

    @PutMapping("/addresses/{id}")
    AddressDto updateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody AddressDto req) {
        return service.saveAddress(Long.valueOf(jwt.getSubject()), id, req);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.deleteAddress(Long.valueOf(jwt.getSubject()), id);

    }
}
