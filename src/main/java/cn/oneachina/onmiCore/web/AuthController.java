package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.database.DatabaseManager;
import io.javalin.http.Context;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthController {
    private final DatabaseManager db;
    private final AuthService authService;
    private final Map<String, String> bindTokens = new ConcurrentHashMap<>();
    private final Map<String, SessionCode> sessionCodes = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private static final long SESSION_CODE_TTL_MS = 10_000;

    private record SessionCode(String uuid, long expiresAt) {}

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

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO web_users (player_uuid, player_name, username, password_hash, token_encrypted, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))")) {
                ps.setString(1, uuid);
                ps.setString(2, Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                ps.setString(3, username);
                ps.setString(4, passwordHash);
                ps.setString(5, rawToken);
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
                    "SELECT username, password_hash FROM web_users WHERE player_uuid = ?")) {
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
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE web_users SET token_encrypted = ?, last_login = datetime('now') WHERE player_uuid = ?")) {
                        update.setString(1, newToken);
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
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO web_users (player_uuid, player_name, username, password_hash, token_encrypted, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))")) {
                ps.setString(1, uuid);
                ps.setString(2, Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                ps.setString(3, username);
                ps.setString(4, passwordHash);
                ps.setString(5, rawToken);
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
                "SELECT username FROM web_users WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    String redirectUrl = "/#/register?bind_token=" + bindToken + "&uuid=" + uuid;
                    ctx.redirect(redirectUrl);
                    return;
                }
                String sessionCode = createSessionCode(uuid);
                String dashboardUrl = "/#/dashboard?session=" + sessionCode;
                ctx.redirect(dashboardUrl);
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public void sessionLogin(Context ctx) {
        String sessionCode = ctx.formParam("session");
        if (sessionCode == null) {
            ctx.status(400).json(Map.of("error", "Missing session code"));
            return;
        }
        SessionCode code = sessionCodes.remove(sessionCode);
        if (code == null || System.currentTimeMillis() > code.expiresAt()) {
            ctx.status(401).json(Map.of("error", "Invalid or expired session code"));
            return;
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT username FROM web_users WHERE player_uuid = ?")) {
            ps.setString(1, code.uuid());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    ctx.status(401).json(Map.of("error", "User not found"));
                    return;
                }
                String username = rs.getString("username");
                String token = authService.generateToken(code.uuid(), username);
                ctx.json(Map.of("token", token, "uuid", code.uuid(), "username", username));
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
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        String bindToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        bindTokens.put(bindToken, uuid);
        return bindToken;
    }

    private String createSessionCode(String uuid) {
        byte[] code = new byte[16];
        secureRandom.nextBytes(code);
        String sessionCode = Base64.getUrlEncoder().withoutPadding().encodeToString(code);
        sessionCodes.put(sessionCode, new SessionCode(uuid, System.currentTimeMillis() + SESSION_CODE_TTL_MS));
        return sessionCode;
    }
}
