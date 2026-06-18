package cn.oneachina.onmiCore.web;

import cn.oneachina.onmiCore.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthController {
    private final DatabaseManager databaseManager;
    private final AuthService authService;
    private final Gson gson;

    public AuthController(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.authService = new AuthService();
        this.gson = new Gson();
    }

    public void bind(Context ctx) {
        String uuidStr = ctx.queryParam("uuid");
        if (uuidStr == null || uuidStr.isEmpty()) {
            try {
                JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);
                if (body != null && body.has("uuid")) {
                    uuidStr = body.get("uuid").getAsString();
                }
            } catch (Exception ignored) {
            }
        }

        if (uuidStr == null || uuidStr.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "uuid is required"));
            return;
        }

        UUID playerUuid;
        try {
            playerUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid uuid"));
            return;
        }

        String sql = "SELECT player_uuid, player_name FROM web_users WHERE player_uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String playerName = rs.getString("player_name");
                    String token = authService.generateToken(playerUuid, playerName);
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", token);
                    data.put("uuid", playerUuid.toString());
                    data.put("playerName", playerName);
                    ctx.json(Map.of("success", true, "data", data, "message", "ok"));
                } else {
                    ctx.status(404).json(Map.of("success", false, "message", "not_registered"));
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public void register(Context ctx) {
        JsonObject body;
        try {
            body = gson.fromJson(ctx.body(), JsonObject.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid json"));
            return;
        }

        if (body == null || !body.has("uuid") || !body.has("username") || !body.has("password") || !body.has("playerName")) {
            ctx.status(400).json(Map.of("success", false, "message", "missing required fields: uuid, username, password, playerName"));
            return;
        }

        String uuidStr = body.get("uuid").getAsString();
        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        String playerName = body.get("playerName").getAsString();

        UUID playerUuid;
        try {
            playerUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid uuid"));
            return;
        }

        String checkSql = "SELECT id FROM web_users WHERE player_uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctx.status(409).json(Map.of("success", false, "message", "already registered"));
                    return;
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", e.getMessage()));
            return;
        }

        String passwordHash = authService.hashPassword(password);
        String randomToken = UUID.randomUUID().toString();
        String encryptedToken = authService.encryptToken(randomToken);
        String now = Instant.now().toString();

        String insertSql = "INSERT INTO web_users (player_uuid, player_name, username, password_hash, token_encrypted, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, username);
            ps.setString(4, passwordHash);
            ps.setString(5, encryptedToken);
            ps.setString(6, now);
            ps.executeUpdate();
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", e.getMessage()));
            return;
        }

        String token = authService.generateToken(playerUuid, playerName);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("uuid", playerUuid.toString());
        data.put("playerName", playerName);
        ctx.status(201).json(Map.of("success", true, "data", data, "message", "registered"));
    }

    public void login(Context ctx) {
        JsonObject body;
        try {
            body = gson.fromJson(ctx.body(), JsonObject.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("success", false, "message", "invalid json"));
            return;
        }

        if (body == null || !body.has("username") || !body.has("password")) {
            ctx.status(400).json(Map.of("success", false, "message", "missing required fields: username, password"));
            return;
        }

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        String sql = "SELECT player_uuid, player_name, password_hash FROM web_users WHERE username = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String passwordHash = rs.getString("password_hash");
                    if (authService.verifyPassword(password, passwordHash)) {
                        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                        String playerName = rs.getString("player_name");

                        String updateSql = "UPDATE web_users SET last_login = ? WHERE username = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setString(1, Instant.now().toString());
                            updatePs.setString(2, username);
                            updatePs.executeUpdate();
                        }

                        String token = authService.generateToken(playerUuid, playerName);
                        Map<String, Object> data = new HashMap<>();
                        data.put("token", token);
                        data.put("uuid", playerUuid.toString());
                        data.put("playerName", playerName);
                        ctx.json(Map.of("success", true, "data", data, "message", "ok"));
                    } else {
                        ctx.status(401).json(Map.of("success", false, "message", "invalid password"));
                    }
                } else {
                    ctx.status(404).json(Map.of("success", false, "message", "user not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public void refresh(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of("success", false, "message", "missing or invalid authorization header"));
            return;
        }

        String token = authHeader.substring(7);
        try {
            var claims = authService.validateToken(token);
            UUID playerUuid = UUID.fromString(claims.get("uuid", String.class));
            String playerName = claims.get("name", String.class);

            String newToken = authService.generateToken(playerUuid, playerName);
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("uuid", playerUuid.toString());
            data.put("playerName", playerName);
            ctx.json(Map.of("success", true, "data", data, "message", "ok"));
        } catch (Exception e) {
            ctx.status(401).json(Map.of("success", false, "message", "invalid or expired token"));
        }
    }
}
