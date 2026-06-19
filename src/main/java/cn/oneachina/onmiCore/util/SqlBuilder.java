package cn.oneachina.onmiCore.util;

import java.util.ArrayList;
import java.util.List;

public final class SqlBuilder {
    private final StringBuilder sql;
    private final List<Object> params = new ArrayList<>();
    private boolean hasWhere = false;

    private SqlBuilder(String base) {
        this.sql = new StringBuilder(base);
    }

    public static SqlBuilder select(String columns, String table) {
        return new SqlBuilder("SELECT " + columns + " FROM " + table);
    }

    public static SqlBuilder select(String columns, String table, String alias) {
        return new SqlBuilder("SELECT " + columns + " FROM " + table + " " + alias);
    }

    public static SqlBuilder count(String table) {
        return new SqlBuilder("SELECT COUNT(*) FROM " + table);
    }

    public static SqlBuilder count(String table, String alias) {
        return new SqlBuilder("SELECT COUNT(*) FROM " + table + " " + alias);
    }

    public static SqlBuilder deleteFrom(String table) {
        return new SqlBuilder("DELETE FROM " + table);
    }

    public SqlBuilder where(String clause, Object... values) {
        if (!hasWhere) {
            sql.append(" WHERE ");
            hasWhere = true;
        } else {
            sql.append(" AND ");
        }
        sql.append(clause);
        for (Object v : values) {
            params.add(v);
        }
        return this;
    }

    public SqlBuilder and(String clause, Object... values) {
        return where(clause, values);
    }

    public SqlBuilder orderBy(String order) {
        sql.append(" ORDER BY ").append(order);
        return this;
    }

    public SqlBuilder limit(int limit) {
        sql.append(" LIMIT ?");
        params.add(limit);
        return this;
    }

    public SqlBuilder offset(int offset) {
        sql.append(" OFFSET ?");
        params.add(offset);
        return this;
    }

    public String build() {
        return sql.toString();
    }

    public List<Object> getParams() {
        return params;
    }

    public int paramCount() {
        return params.size();
    }
}
