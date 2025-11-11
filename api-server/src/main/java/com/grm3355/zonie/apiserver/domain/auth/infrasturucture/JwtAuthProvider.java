package com.grm3355.zonie.apiserver.domain.auth.infrasturucture;

import com.grm3355.zonie.apiserver.domain.auth.domain.AuthProvider;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class JwtAuthProvider implements AuthProvider {
    private static final int SECOND_FACTOR = 60;
    private static final int MILLISECOND_FACTOR = 1000;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtAuthProvider(String secretKey, long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String provide(User user) {
        Date now = new Date();
        return Jwts.builder()
                .claim("userId", user.getId())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMinutes * SECOND_FACTOR * MILLISECOND_FACTOR))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
