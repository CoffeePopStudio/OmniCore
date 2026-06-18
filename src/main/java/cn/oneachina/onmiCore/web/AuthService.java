package cn.oneachina.onmiCore.web;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

public final class AuthService {

    private static final long TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days
    private static final int AES_KEY_LENGTH = 16; // 128-bit

    private final SecretKey jwtKey;
    private final SecretKey aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService() {
        JavaPlugin plugin = JavaPlugin.getPlugin(cn.oneachina.onmiCore.OnmiCore.class);
        String secret = plugin.getConfig().getString("web-panel.jwt-secret", "");
        if (secret.isEmpty()) {
            byte[] keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            secret = Base64.getEncoder().encodeToString(keyBytes);
            plugin.getConfig().set("web-panel.jwt-secret", secret);
            plugin.saveConfig();
        }
        byte[] keyData = secret.getBytes(StandardCharsets.UTF_8);
        if (keyData.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyData, 0, padded, 0, Math.min(keyData.length, 32));
            keyData = padded;
        }
        this.jwtKey = Keys.hmacShaKeyFor(keyData);

        byte[] aesKeyData = new byte[AES_KEY_LENGTH];
        System.arraycopy(keyData, 0, aesKeyData, 0, Math.min(keyData.length, AES_KEY_LENGTH));
        this.aesKey = new SecretKeySpec(aesKeyData, "AES");
    }

    public String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    public boolean verifyPassword(String password, String hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }

    public String generateToken(String uuid, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(uuid)
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TOKEN_EXPIRY_MS))
                .signWith(jwtKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public String getUuidFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }

    public String refreshToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;
        return generateToken(claims.getSubject(), claims.get("username", String.class));
    }

    public String encryptAES(String data) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return data;
        }
    }

    public String decryptAES(String encryptedData) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedData;
        }
    }

    public String generateBindToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
