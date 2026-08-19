package com.ankur.database.postgres.sqli;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SqlInjectionDemoService {

    private final JdbcTemplate jdbc;

    public SqlInjectionDemoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Seed a demo accounts table on startup so login examples work
    @PostConstruct
    void init() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS sqli_accounts (
                id       SERIAL PRIMARY KEY,
                username VARCHAR(100),
                password VARCHAR(100),
                role     VARCHAR(20)
            )
            """);
        jdbc.execute("""
            INSERT INTO sqli_accounts (username, password, role)
            SELECT * FROM (VALUES
                ('admin',  'supersecret123', 'admin'),
                ('alice',  'alice_pass',     'user'),
                ('bob',    'bob_pass',       'user')
            ) AS v(username, password, role)
            WHERE NOT EXISTS (SELECT 1 FROM sqli_accounts)
            """);
    }

    // ---------------------------------------------------------------
    // VULNERABLE — string concatenation directly into SQL
    // ---------------------------------------------------------------

    public List<Map<String, Object>> vulnerableSearch(String name) {
        // BAD: user input lands verbatim in the query string
        String sql = "SELECT id, name, email, status FROM users WHERE name = '" + name + "'";
        return jdbc.queryForList(sql);
    }

    public Map<String, Object> vulnerableLogin(String username, String password) {
        // BAD: classic auth-bypass target
        String sql = "SELECT id, username, role FROM sqli_accounts"
                   + " WHERE username = '" + username + "'"
                   + " AND   password = '" + password + "'";
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) {
            return Map.of("authenticated", false, "message", "Invalid credentials");
        }
        return Map.of("authenticated", true, "account", rows.get(0));
    }

    // ---------------------------------------------------------------
    // SAFE — parameterised queries; input is never concatenated
    // ---------------------------------------------------------------

    public List<Map<String, Object>> safeSearch(String name) {
        // GOOD: ? placeholder — JDBC driver handles quoting/escaping
        String sql = "SELECT id, name, email, status FROM users WHERE name = ?";
        return jdbc.queryForList(sql, name);
    }

    public Map<String, Object> safeLogin(String username, String password) {
        // GOOD: both values passed as bind parameters
        String sql = "SELECT id, username, role FROM sqli_accounts"
                   + " WHERE username = ? AND password = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, username, password);
        if (rows.isEmpty()) {
            return Map.of("authenticated", false, "message", "Invalid credentials");
        }
        return Map.of("authenticated", true, "account", rows.get(0));
    }
}