package cn.oneachina.onmiCore.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseUtil {

    private static final int STREAMING_FETCH_SIZE = Integer.MIN_VALUE;

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> List<T> query(Connection conn, String sql, List<Object> params, RowMapper<T> mapper) throws SQLException {
        return query(conn, sql, params, mapper, false);
    }

    public static <T> List<T> query(Connection conn, String sql, List<Object> params, RowMapper<T> mapper, boolean streaming) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (streaming) {
                ps.setFetchSize(STREAMING_FETCH_SIZE);
            }
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        }
    }

    public static <T> List<T> queryAll(Connection conn, String sql, RowMapper<T> mapper) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
            return results;
        }
    }

    public static <T> T queryOne(Connection conn, String sql, List<Object> params, RowMapper<T> mapper) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper.map(rs) : null;
            }
        }
    }

    public static int count(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public static int execute(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            return ps.executeUpdate();
        }
    }

    public static void setParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof java.time.Instant instant) {
                ps.setTimestamp(i + 1, java.sql.Timestamp.from(instant));
            } else {
                ps.setObject(i + 1, param);
            }
        }
    }

    private DatabaseUtil() {}
}
