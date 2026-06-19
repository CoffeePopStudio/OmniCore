package cn.oneachina.onmiCore.web;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

public final class AuthService {

    private static final long TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L;
    private static final String ENV_SECRET_KEY = "ONMICORE_JWT_SECRET";

    private final SecretKey jwtKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService() {
        JavaPlugin plugin = JavaPlugin.getPlugin(cn.oneachina.onmiCore.OnmiCore.class);
        byte[] keyBytes = loadOrGenerateSecret(plugin);
        this.jwtKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] loadOrGenerateSecret(JavaPlugin plugin) {
        String envSecret = System.getenv(ENV_SECRET_KEY);
        if (envSecret != null && !envSecret.isEmpty()) {
            byte[] decoded = Base64.getDecoder().decode(envSecret);
            if (decoded.length >= 32) {
                return decoded;
            }
            byte[] key = new byte[32];
            System.arraycopy(decoded, 0, key, 0, Math.min(decoded.length, 32));
            return key;
        }

        File secretFile = new File(plugin.getDataFolder(), "secret.key");
        if (secretFile.exists()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(Files.readString(secretFile.toPath()).trim());
                if (decoded.length >= 32) return decoded;
                byte[] key = new byte[32];
                System.arraycopy(decoded, 0, key, 0, Math.min(decoded.length, 32));
                return key;
            } catch (Exception e) {
                plugin.getSLF4JLogger().warn("Failed to read secret.key, generating new one");
            }
        }

        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String encoded = Base64.getEncoder().encodeToString(keyBytes);
        try {
            secretFile.getParentFile().mkdirs();
            Files.writeString(secretFile.toPath(), encoded);
            plugin.getSLF4JLogger().info("Generated new JWT secret key at {}", secretFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("Failed to save secret.key", e);
        }
        return keyBytes;
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
}
