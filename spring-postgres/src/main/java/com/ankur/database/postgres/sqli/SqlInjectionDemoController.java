package com.ankur.database.postgres.sqli;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SQL Injection demo — deliberately insecure endpoints sit under /sqli/vulnerable/*.
 * Safe counterparts under /sqli/safe/* show the parameterised fix.
 *
 * See sql_injection_demo.md for attack payloads to try.
 */
@RestController
@RequestMapping("/sqli")
public class SqlInjectionDemoController {

    private final SqlInjectionDemoService service;

    public SqlInjectionDemoController(SqlInjectionDemoService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------
    // VULNERABLE endpoints
    // ------------------------------------------------------------------

    @GetMapping("/vulnerable/search")
    public ResponseEntity<List<Map<String, Object>>> vulnerableSearch(@RequestParam String name) {
        return ResponseEntity.ok(service.vulnerableSearch(name));
    }

    @GetMapping("/vulnerable/login")
    public ResponseEntity<Map<String, Object>> vulnerableLogin(
            @RequestParam String username,
            @RequestParam String password) {
        return ResponseEntity.ok(service.vulnerableLogin(username, password));
    }

    // ------------------------------------------------------------------
    // SAFE endpoints (parameterised queries)
    // ------------------------------------------------------------------

    @GetMapping("/safe/search")
    public ResponseEntity<List<Map<String, Object>>> safeSearch(@RequestParam String name) {
        return ResponseEntity.ok(service.safeSearch(name));
    }

    @GetMapping("/safe/login")
    public ResponseEntity<Map<String, Object>> safeLogin(
            @RequestParam String username,
            @RequestParam String password) {
        return ResponseEntity.ok(service.safeLogin(username, password));
    }
}