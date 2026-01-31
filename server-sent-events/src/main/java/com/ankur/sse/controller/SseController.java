package com.ankur.sse.controller;

import com.ankur.sse.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SseController {

    private final SseNotificationService sseNotificationService;

    /**
     * Establish SSE connection for a user
     * Example: GET /api/sse/connect?userId=user123
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String userId) {
        log.info("SSE connection request from user: {}", userId);
        return sseNotificationService.registerConnection(userId);
    }

    /**
     * Get active connection count for a user
     */
    @GetMapping("/connections/{userId}")
    public int getActiveConnections(@PathVariable String userId) {
        return sseNotificationService.getActiveConnections(userId);
    }
}