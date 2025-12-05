package com.gymapp.gym_management.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    // ⚙️ Secret key from environment variable (production-safe)
    @Value("${jwt.secret:MySuperSecretKeyForJWTGeneration12345}")
    private String SECRET_KEY;

    // ⏱️ Token expiry duration — 6 hours
    @Value("${jwt.expiration:21600000}")
    private long EXPIRATION_TIME;

    // ✅ Generate Token with expiry
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    // ✅ Extract username from token
    public String extractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ✅ Check if token has expired
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true; // token already expired
        }
    }

    // ✅ Validate token (checks username + expiry)
    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (username.equals(extractedUsername) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            System.out.println("❌ Token expired: " + e.getMessage());
            return false;
        } catch (JwtException e) {
            System.out.println("❌ Invalid token: " + e.getMessage());
            return false;
        }
    }
}
