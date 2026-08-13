package com.shiftscheduler.server.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

public class JwtTokenUtil {
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            "your-secret-key-change-in-production-environment-please-keep-it-long-enough-for-hs512".getBytes(StandardCharsets.UTF_8)
    );
    private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

    public static String generateToken(Long staffId, String staffCode, String roleLevel) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("staffId", staffId);
        claims.put("staffCode", staffCode);
        claims.put("roleLevel", roleLevel);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims validateToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
            .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static Long getStaffIdFromToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        return claims.get("staffId", Long.class);
    }

    public static String getStaffCodeFromToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        return claims.get("staffCode", String.class);
    }

    public static String getRoleLevelFromToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        return claims.get("roleLevel", String.class);
    }

    public static Date getIssuedAtFromToken(String token) throws JwtException {
        return validateToken(token).getIssuedAt();
    }
}
