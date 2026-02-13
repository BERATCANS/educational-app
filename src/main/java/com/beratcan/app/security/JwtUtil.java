package com.beratcan.app.security;

import com.beratcan.app.service.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Token utility sınıfı
 * Token oluşturma, validation ve blacklist yönetimi
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationTime;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Access Token oluştur (kısa ömürlü, her API çağrısında kullanılır)
     * 
     * @param username        Kullanıcı adı
     * @param userId          Kullanıcı ID
     * @param activeSessionId Aktif oturum ID'si
     * @return Access JWT token
     */
    public String generateAccessToken(String username, Long userId, String activeSessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        log.debug("Generating ACCESS token for user: {} (ID: {}), session: {}, expires at: {}", username, userId, activeSessionId, expiryDate);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("sid", activeSessionId) // Oturum ID'sini ekle
                .claim("type", "ACCESS") // Token tipini belirt
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token oluştur (uzun ömürlü, token yenileme için kullanılır)
     * 
     * @param username        Kullanıcı adı
     * @param userId          Kullanıcı ID
     * @param activeSessionId Aktif oturum ID'si
     * @return Refresh JWT token
     */
    public String generateRefreshToken(String username, Long userId, String activeSessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationTime);

        log.debug("Generating REFRESH token for user: {} (ID: {}), session: {}, expires at: {}", username, userId, activeSessionId, expiryDate);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("sid", activeSessionId) // Oturum ID'sini ekle
                .claim("type", "REFRESH") // Token tipini belirt
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Backward compatibility için eski metod adını tut
     * 
     * @deprecated generateAccessToken kullan
     */
    @Deprecated
    public String generateToken(String username, Long userId) {
        return generateAccessToken(username, userId, null);
    }

    /**
     * Token'dan username çıkar
     * 
     * @param token JWT token
     * @return username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Token'dan userId çıkar
     * 
     * @param token JWT token
     * @return userId
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Token'dan session ID çıkar
     * 
     * @param token JWT token
     * @return session ID (sid)
     */
    public String extractSessionId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("sid", String.class);
    }

    /**
     * Token'ın expiration tarihini al
     * 
     * @param token JWT token
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Token'ın issued at tarihini al
     * 
     * @param token JWT token
     * @return issued at date
     */
    public Date getIssuedAtDateFromToken(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Token'dan belirli bir claim çıkar
     * 
     * @param token          JWT token
     * @param claimsResolver claim resolver function
     * @return claim değeri
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Token'dan tüm claims'leri çıkar
     * 
     * @param token JWT token
     * @return claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Token expire olmuş mu kontrol et
     * 
     * @param token JWT token
     * @return true ise expire olmuş
     */
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Token blacklist'te mi kontrol et
     * 
     * @param token JWT token
     * @return true ise blacklist'te
     */
    public Boolean isTokenBlacklisted(String token) {
        return tokenBlacklistService.isTokenBlacklisted(token);
    }

    /**
     * Token'ı blacklist'e ekle (logout için)
     * 
     * @param token JWT token
     */
    public void blacklistToken(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            tokenBlacklistService.blacklistToken(token, expiration);
            log.info("Token blacklisted successfully");
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    /**
     * Token geçerli mi kontrol et (basit - sadece signature ve expiry)
     * 
     * @param token JWT token
     * @return true ise geçerli
     */
    public boolean validateToken(String token) {
        try {
            // Blacklist kontrolü
            if (isTokenBlacklisted(token)) {
                log.warn("Token validation failed: Token is blacklisted");
                return false;
            }

            // Signature ve expiry kontrolü
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            log.debug("Token validated successfully");
            return true;

        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Token geçerli mi kontrol et (username ile cross-check)
     * 
     * @param token    JWT token
     * @param username Beklenen username
     * @return true ise geçerli
     */
    public Boolean validateToken(String token, String username) {
        try {
            // Blacklist kontrolü
            if (isTokenBlacklisted(token)) {
                log.warn("Token validation failed: Token is blacklisted");
                return false;
            }

            final String tokenUsername = extractUsername(token);
            boolean isValid = (tokenUsername.equals(username) && !isTokenExpired(token));

            if (isValid) {
                log.debug("Token validated successfully for user: {}", username);
            } else {
                log.warn("Token validation failed for user: {}", username);
            }

            return isValid;

        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Token geçerli mi kontrol et (UserDetails ile tam kontrol - Session ID dahil)
     * 
     * @param token       JWT token
     * @param userDetails UserDetails (DB'den gelen güncel bilgi)
     * @return true ise geçerli
     */
    public Boolean validateToken(String token, UserDetailsImpl userDetails) {
        try {
            // 1. Temel validasyon (imza, süre, username)
            if (!validateToken(token, userDetails.getUsername())) {
                return false;
            }

            // 2. Session ID kontrolü (Concurrent Session Prevention)
            String tokenSessionId = extractSessionId(token);
            String dbSessionId = userDetails.getActiveSessionId();

            // Eğer token'da session ID varsa, DB'deki ile eşleşmek ZORUNDA
            if (tokenSessionId != null) {
                if (dbSessionId == null || !tokenSessionId.equals(dbSessionId)) {
                    log.warn("Session mismatch! Token sid: {}, DB session: {}. User: {}",
                            tokenSessionId, dbSessionId, userDetails.getUsername());
                    return false;
                }
            } else {
                 // Geriye dönük uyumluluk: Eğer token'da session ID yoksa (eski token),
                 // ve DB'de session ID varsa, bu token GEÇERSİZ olmalı mı?
                 // Güvenlik politikası: Strict Single Session ise EVET, geçersiz olmalı.
                 // Ancak geçiş sürecinde eski tokenları öldürmek istiyoruz.
                 if (dbSessionId != null) {
                     log.warn("Legacy token without session ID used while active session exists. User: {}",
                             userDetails.getUsername());
                     return false; 
                 }
            }

            return true;
        } catch (Exception e) {
            log.error("Token/User validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Token access token mı kontrol et
     * 
     * @param token JWT token
     * @return true ise access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            return "ACCESS".equals(type);
        } catch (Exception e) {
            log.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Token refresh token mı kontrol et
     * 
     * @param token JWT token
     * @return true ise refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            return "REFRESH".equals(type);
        } catch (Exception e) {
            log.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Token expiration süresini saniye cinsinden al (frontend için)
     * 
     * @return expiration time in seconds
     */
    public long getExpirationTimeInSeconds() {
        return expirationTime / 1000;
    }
}
