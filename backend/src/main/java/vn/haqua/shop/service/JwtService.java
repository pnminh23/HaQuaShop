package vn.haqua.shop.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import vn.haqua.shop.config.AppProperties;
import vn.haqua.shop.entity.User;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AppProperties props;

    public String accessToken(User u) {
        return encode(u, "access", props.jwt().accessTtl().toSeconds());
    }

    public String refreshToken(User u) {
        return encode(u, "refresh", props.jwt().refreshTtl().toSeconds());
    }

    public long accessTtlSeconds() {
        return props.jwt().accessTtl().toSeconds();
    }

    /** Trả userId nếu là refresh token hợp lệ. */
    public Long parseRefresh(String token) {
        var jwt = decoder.decode(token);
        if (!"refresh".equals(jwt.getClaimAsString("typ"))) {
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh token không hợp lệ");
        }
        return Long.valueOf(jwt.getSubject());
    }

    private String encode(User u, String type, long ttlSeconds) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(u.getId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .claim("role", u.getRole().name())
                .claim("typ", type)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

    }
}
