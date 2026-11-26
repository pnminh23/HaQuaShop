package vn.haqua.shop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.enums.Role;
import vn.haqua.shop.enums.UserStatus;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.OrderRepository;
import vn.haqua.shop.repository.UserRepository;

/** UC20 – Quản lý khách hàng. */
@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CustomerAdminController {

    record CustomerRow(Long id, String fullName, String phone, String email, long orderCount, long totalSpent,
                       int greenPoints, UserStatus status) {
    }

    private final UserRepository users;
    private final OrderRepository orders;

    @GetMapping
    @Transactional(readOnly = true)
    PageDto<CustomerRow> list(@RequestParam(defaultValue = "") String q,
                              @RequestParam(required = false) UserStatus status,
                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        var result = status != null ? users.searchCustomersByStatus(q.trim(), status, pageable)
                : q.isBlank() ? users.findByRole(Role.CUSTOMER, pageable) : users.searchCustomers(q.trim(), pageable);
        return PageDto.of(result, u -> {
            Object[] stats = orders.statsForUser(u.getId()).get(0);
            return new CustomerRow(u.getId(), u.getFullName(), u.getPhone(), u.getEmail(), (Long) stats[0],
                    (Long) stats[1], u.getGreenPoints(), u.getStatus());
        });
    }

    @PostMapping("/{id}/lock")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void lock(@PathVariable Long id) {
        setStatus(id, UserStatus.LOCKED);
    }

    @PostMapping("/{id}/unlock")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unlock(@PathVariable Long id) {
        setStatus(id, UserStatus.ACTIVE);
    }

    private void setStatus(Long id, UserStatus status) {
        var u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy khách hàng"));
        if (u.getRole() != Role.CUSTOMER) {
            throw ApiException.forbidden("Chỉ khóa/mở khóa tài khoản khách hàng");
        }
        u.setStatus(status);

    }
}
