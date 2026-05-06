package com.rps.samaj.security;

import com.rps.samaj.config.SamajProperties;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ADMIN = "adm";
    public static final String CLAIM_TYP = "typ";
    public static final String TYP_ACCESS = "ACCESS";
    public static final String TYP_REFRESH = "REFRESH";
    public static final String TYP_GOOGLE_SIGNUP = "GOOGLE_SIGNUP";

    private final SamajProperties properties;
    private final SecretKey key;

    public JwtService(SamajProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("samaj.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getJwt().getAccessTtlMinutes() * 60L);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_ADMIN, adminCapable(user))
                .claim(CLAIM_TYP, TYP_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getJwt().getRefreshTtlDays() * 86400L);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_TYP, TYP_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public ParsedJwt parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("Missing subject");
        }
        UUID userId = UUID.fromString(sub);
        String typ = claims.get(CLAIM_TYP, String.class);
        String roleStr = claims.get(CLAIM_ROLE, String.class);
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.USER;
        Boolean adm = claims.get(CLAIM_ADMIN, Boolean.class);
        return new ParsedJwt(userId, typ, role, Boolean.TRUE.equals(adm));
    }

    public boolean isExpired(String token) {
        try {
            parse(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public static boolean adminCapable(User user) {
        return user.isParentAdmin()
                || user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.MODERATOR;
    }

    public long accessTtlSeconds() {
        return properties.getJwt().getAccessTtlMinutes() * 60L;
    }

    public String createGoogleSignupToken(String googleId, String email, String name, String picture) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);
        return Jwts.builder()
                .subject(googleId)
                .claim("email", email)
                .claim("name", name != null ? name : "")
                .claim("pic", picture != null ? picture : "")
                .claim(CLAIM_TYP, TYP_GOOGLE_SIGNUP)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public GoogleSignupClaims parseGoogleSignupToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYP_GOOGLE_SIGNUP.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new IllegalArgumentException("Not a Google signup token");
        }
        return new GoogleSignupClaims(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("name", String.class),
                claims.get("pic", String.class)
        );
    }

    public record ParsedJwt(UUID userId, String typ, UserRole role, boolean adminCapable) {
    }

    public record GoogleSignupClaims(String googleId, String email, String name, String picture) {
    }
}
