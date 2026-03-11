package com.example.localchat.infrastructure.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

@Slf4j
@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    private Key signKey;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith((javax.crypto.SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSignInKey() {
        if (signKey != null) {
            return signKey;
        }
        
        if (secretKey == null || secretKey.isBlank()) {
            log.error("JWT Secret Key is NULL or empty!");
            throw new RuntimeException("JWT Secret Key is not configured correctly");
        }
        
        String trimmedKey = secretKey.trim();
        log.debug("Initializing JwtService with secret key of length: {}", trimmedKey.length());
        
        try {
            byte[] keyBytes = Decoders.BASE64.decode(trimmedKey);
            log.debug("Key decoded successfully as Base64. Bytes: {}", keyBytes.length);
            this.signKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.warn("Failed to decode secret key as Base64, falling back to raw bytes. Error: {}", e.getMessage());
            byte[] keyBytes = trimmedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            this.signKey = Keys.hmacShaKeyFor(keyBytes);
        }
        
        return signKey;
    }
}
