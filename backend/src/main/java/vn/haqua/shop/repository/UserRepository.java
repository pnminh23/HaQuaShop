package vn.haqua.shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.User;
import vn.haqua.shop.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCaseOrPhone(String email, String phone);

    boolean existsByEmailIgnoreCaseOrPhone(String email, String phone);

    long countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role role, java.time.Instant from, java.time.Instant to);

    org.springframework.data.domain.Page<User> findByRole(Role role, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select u from User u where u.role = 'CUSTOMER' and
            (lower(u.fullName) like lower(concat('%', :q, '%')) or u.phone like concat('%', :q, '%')
             or lower(u.email) like lower(concat('%', :q, '%')))""")
    org.springframework.data.domain.Page<User> searchCustomers(String q, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select u from User u where u.role = 'CUSTOMER' and u.status = :status and
            (lower(u.fullName) like lower(concat('%', :q, '%')) or u.phone like concat('%', :q, '%')
             or lower(u.email) like lower(concat('%', :q, '%')))""")
    org.springframework.data.domain.Page<User> searchCustomersByStatus(String q, vn.haqua.shop.enums.UserStatus status,
                                                                      org.springframework.data.domain.Pageable pageable);

}
