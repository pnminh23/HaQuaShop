package vn.haqua.shop.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.Role;
import vn.haqua.shop.enums.UserStatus;
import vn.haqua.shop.exception.ApiException;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    private int greenPoints;

    private int failedLogins;

    private Instant lockedUntil;

    public User(String email, String phone, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public void addPoints(int points) {
        this.greenPoints += points;
    }

    public void redeemPoints(int points) {
        if (points > greenPoints) {
            throw ApiException.badRequest("Không đủ điểm HaQua Xanh");
        }
        this.greenPoints -= points;
    }

    public boolean isLoginLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());

    }
}
