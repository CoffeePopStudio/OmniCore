package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.database.DatabaseManager;
import cn.oneachina.onmiCore.util.PermissionUtil;
import io.javalin.http.Context;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthController {
    private final DatabaseManager db;
    private final AuthService authService;
    private final Map<String, String> bindTokens = new ConcurrentHashMap<>();

    public AuthController(DatabaseManager db, AuthService authService) {
        this.db = db;
        this.authService = authService;
    }

    public void register(Context ctx) {
        try {
            String uuid = ctx.formParam("uuid");
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            if (uuid == null || username == null || password == null) {
                ctx.status(400).json(Map.of("error", "Missing fields"));
                return;
            }
            if (Bukkit.getPlayer(UUID.fromString(uuid)) == null && Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName() == null) {
                ctx.status(400).json(Map.of("error", "Player not found"));
                return;
            }
            try (Connection conn = db.getConnection();
                 PreparedStatement check = conn.prepareStatement("SELECT id FROM web_users WHERE player_uuid = ?")) {
                check.setString(1, uuid);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        ctx.status(409).json(Map.of("error", "Already registered"));
                        return;
                    }
                }
            }
            String passwordHash = authService.hashPassword(password);
            String rawToken = authService.generateToken(uuid, username);
            String encryptedToken = authService.encryptAES(rawToken);

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO web_users (player_uuid, player_name, username, password_hash, token_encrypted, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))")) {
                ps.setString(1, uuid);
                ps.setString(2, Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                ps.setString(3, username);
                ps.setString(4, passwordHash);
                ps.setString(5, encryptedToken);
                ps.executeUpdate();
            }
            ctx.json(Map.of("token", rawToken, "uuid", uuid, "username", username));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void login(Context ctx) {
        try {
            String uuid = ctx.formParam("uuid");
            String password = ctx.formParam("password");
            if (uuid == null || password == null) {
                ctx.status(400).json(Map.of("error", "Missing fields"));
                return;
            }
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, password_hash, token_encrypted FROM web_users WHERE player_uuid = ?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        ctx.status(401).json(Map.of("error", "Not registered"));
                        return;
                    }
                    String hash = rs.getString("password_hash");
                    if (!authService.verifyPassword(password, hash)) {
                        ctx.status(401).json(Map.of("error", "Invalid password"));
                        return;
                    }
                    String username = rs.getString("username");
                    String newToken = authService.generateToken(uuid, username);
                    String encryptedToken = authService.encryptAES(newToken);
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE web_users SET token_encrypted = ?, last_login = datetime('now') WHERE player_uuid = ?")) {
                        update.setString(1, encryptedToken);
                        update.setString(2, uuid);
                        update.executeUpdate();
                    }
                    ctx.json(Map.of("token", newToken, "uuid", uuid, "username", username));
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void bind(Context ctx) {
        String bindToken = ctx.formParam("bind_token");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        if (bindToken == null || username == null || password == null) {
            ctx.status(400).json(Map.of("error", "Missing fields"));
            return;
        }
        String uuid = bindTokens.remove(bindToken);
        if (uuid == null) {
            ctx.status(400).json(Map.of("error", "Invalid or expired bind token"));
            return;
        }
        try {
            try (Connection conn = db.getConnection();
                 PreparedStatement check = conn.prepareStatement("SELECT id FROM web_users WHERE player_uuid = ?")) {
                check.setString(1, uuid);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        ctx.status(409).json(Map.of("error", "Already registered"));
                        return;
                    }
                }
            }
            String passwordHash = authService.hashPassword(password);
            String rawToken = authService.generateToken(uuid, username);
            String encryptedToken = authService.encryptAES(rawToken);
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO web_users (player_uuid, player_name, username, password_hash, token_encrypted, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))")) {
                ps.setString(1, uuid);
                ps.setString(2, Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                ps.setString(3, username);
                ps.setString(4, passwordHash);
                ps.setString(5, encryptedToken);
                ps.executeUpdate();
            }
            ctx.json(Map.of("token", rawToken, "uuid", uuid, "username", username));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void autoLogin(Context ctx) {
        String bindToken = ctx.queryParam("bind_token");
        if (bindToken == null) {
            ctx.status(400).json(Map.of("error", "Missing bind_token"));
            return;
        }
        String uuid = bindTokens.get(bindToken);
        if (uuid == null) {
            ctx.status(400).json(Map.of("error", "Invalid bind token"));
            return;
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT username, token_encrypted FROM web_users WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    String redirectUrl = "/#/register?bind_token=" + bindToken + "&uuid=" + uuid;
                    ctx.redirect(redirectUrl);
                    return;
                }
                String encryptedToken = rs.getString("token_encrypted");
                String token = authService.decryptAES(encryptedToken);
                if (authService.isTokenExpired(token)) {
                    token = authService.refreshToken(token);
                    if (token == null) {
                        ctx.status(401).json(Map.of("error", "Token expired, please login again"));
                        return;
                    }
                    String newEncrypted = authService.encryptAES(token);
                    try (PreparedStatement up = conn.prepareStatement(
                            "UPDATE web_users SET token_encrypted = ? WHERE player_uuid = ?")) {
                        up.setString(1, newEncrypted);
                        up.setString(2, uuid);
                        up.executeUpdate();
                    }
                }
                String dashboardUrl = "/#/dashboard?token=" + token;
                ctx.redirect(dashboardUrl);
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void refresh(Context ctx) {
        String oldToken = ctx.formParam("token");
        if (oldToken == null) {
            ctx.status(400).json(Map.of("error", "Missing token"));
            return;
        }
        String newToken = authService.refreshToken(oldToken);
        if (newToken == null) {
            ctx.status(401).json(Map.of("error", "Invalid or expired token"));
            return;
        }
        ctx.json(Map.of("token", newToken));
    }

    public String createBindToken(String uuid) {
        String bindToken = authService.generateBindToken();
        bindTokens.put(bindToken, uuid);
        return bindToken;
    }
}
