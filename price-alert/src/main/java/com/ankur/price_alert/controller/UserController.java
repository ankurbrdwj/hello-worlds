package com.ankur.price_alert.controller;

import com.ankur.price_alert.dto.UserRequest;
import com.ankur.price_alert.dto.UserResponse;
import com.ankur.price_alert.model.NotificationChannel;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.result.Result;
import com.ankur.price_alert.result.UserError;
import com.ankur.price_alert.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for user management.
 *
 * Two sections:
 *  1. /api/users/**          — classic approach: service throws, GlobalExceptionHandler catches
 *  2. /api/users/result/**   — Result<T,E> approach: errors are values, no exceptions escape
 */

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .notificationChannel(request.getNotificationChannel() != null
                        ? request.getNotificationChannel()
                        : NotificationChannel.EMAIL)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .maxAlerts(request.getMaxAlerts() != null ? request.getMaxAlerts() : 10)
                .build();

        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(createdUser));
    }

    // ==================== READ ====================

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        User user = userService.getByEmail(email);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.findAllUsers().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}/alert-slots")
    public ResponseEntity<Map<String, Object>> getAlertSlots(@PathVariable Long id) {
        int remaining = userService.getRemainingAlertSlots(id);
        boolean canCreate = userService.canCreateMoreAlerts(id);

        return ResponseEntity.ok(Map.of(
                "userId", id,
                "remainingSlots", remaining,
                "canCreateMore", canCreate
        ));
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        User updateData = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .notificationChannel(request.getNotificationChannel())
                .timezone(request.getTimezone())
                .maxAlerts(request.getMaxAlerts() != null ? request.getMaxAlerts() : 0)
                .build();

        User updatedUser = userService.updateUser(id, updateData);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/notification-channel")
    public ResponseEntity<UserResponse> updateNotificationChannel(
            @PathVariable Long id,
            @RequestParam NotificationChannel channel) {
        User updatedUser = userService.updateNotificationChannel(id, channel);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/phone")
    public ResponseEntity<UserResponse> updatePhone(
            @PathVariable Long id,
            @RequestParam String phone) {
        User updatedUser = userService.updatePhone(id, phone);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        User updatedUser = userService.activateUser(id);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        User updatedUser = userService.deactivateUser(id);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESULT PATTERN SECTION  —  /api/users/result/**
    //
    // Every method below demonstrates one or more Result operations.
    // The key idea: the service returns Result<T, UserError>; the controller
    // folds it into a ResponseEntity without any try/catch.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/users/result
     *
     * Demonstrates: flatMap (validation chain) + fold (terminal response)
     *
     * The service chains two flatMap steps:
     *   1. blank-email check → ValidationError
     *   2. duplicate check   → DuplicateEmail
     *
     * fold collapses the final Result into a ResponseEntity.
     * The compiler enforces that both branches (success + every error variant)
     * are handled — nothing falls through silently.
     *
     * Try it:
     *   POST /api/users/result              { "email": "alice@x.com", "name": "Alice" }  → 201
     *   POST /api/users/result (duplicate)  { "email": "alice@x.com", "name": "Bob"   }  → 409
     *   POST /api/users/result (blank)      { "email": "",            "name": "X"      }  → 400
     */
    @PostMapping("/result")
    public ResponseEntity<?> resultCreateUser(@RequestBody UserRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .notificationChannel(request.getNotificationChannel() != null
                        ? request.getNotificationChannel() : NotificationChannel.EMAIL)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .maxAlerts(request.getMaxAlerts() != null ? request.getMaxAlerts() : 10)
                .build();

        return userService.resultCreateUser(user)
                .fold(
                        // onSuccess: wrap the saved user in a 201
                        created -> ResponseEntity.status(HttpStatus.CREATED)
                                                 .body(UserResponse.fromUser(created)),
                        // onError: map each typed error to the right HTTP status + message
                        error   -> ResponseEntity.status(toHttpStatus(error))
                                                 .body(Map.of("error", error.message()))
                );
    }

    /**
     * GET /api/users/result/{id}
     *
     * Demonstrates: fold as the sole terminal operation
     *
     * No try/catch, no if/else. fold handles both shapes with equal weight.
     *
     * Try it:
     *   GET /api/users/result/1      → 200 with user JSON
     *   GET /api/users/result/99999  → 404 { "error": "User not found: 99999" }
     */
    @GetMapping("/result/{id}")
    public ResponseEntity<?> resultGetUser(@PathVariable Long id) {
        return userService.resultGetById(id)
                .fold(
                        user  -> ResponseEntity.ok(UserResponse.fromUser(user)),
                        error -> ResponseEntity.status(toHttpStatus(error))
                                               .body(Map.of("error", error.message()))
                );
    }

    /**
     * GET /api/users/result/{id}/active
     *
     * Demonstrates: flatMap chaining — two sequential checks, each can fail
     *
     * The service uses flatMap internally:
     *   resultGetById(id)                             // can fail: NotFound
     *     .flatMap(user -> activeCheck(user))         // can fail: InactiveAccount
     *
     * The controller just folds the final result — it doesn't care HOW MANY
     * steps were chained; it only sees Success or Failure.
     *
     * Try it:
     *   GET /api/users/result/1/active       → 200 (if user exists and is active)
     *   GET /api/users/result/99999/active   → 404 NotFound
     *   PATCH /api/users/2/deactivate, then:
     *   GET /api/users/result/2/active       → 403 InactiveAccount
     */
    @GetMapping("/result/{id}/active")
    public ResponseEntity<?> resultGetActiveUser(@PathVariable Long id) {
        return userService.resultGetActiveById(id)
                .fold(
                        user  -> ResponseEntity.ok(UserResponse.fromUser(user)),
                        error -> ResponseEntity.status(toHttpStatus(error))
                                               .body(Map.of("error", error.message()))
                );
    }

    /**
     * GET /api/users/result/email/{email}
     *
     * Demonstrates: peekSuccess + peekError for logging, then fold
     *
     * peek* methods let you attach side-effects (logging, metrics) without
     * breaking the chain. They return the original Result unchanged — they
     * are purely observational.
     *
     * Think of them as wiretaps on the railway: the train keeps moving, but
     * you get to inspect the cargo at each station.
     *
     * Try it:
     *   GET /api/users/result/email/alice@x.com  → 200 + INFO log
     *   GET /api/users/result/email/ghost@x.com  → 404 + WARN log
     */
    @GetMapping("/result/email/{email}")
    public ResponseEntity<?> resultGetUserByEmail(@PathVariable String email) {
        return userService.resultGetByEmail(email)
                .peekSuccess(user -> log.info("User lookup succeeded: id={}, email={}", user.getId(), user.getEmail()))
                .peekError(error -> log.warn("User lookup failed: {}", error.message()))
                .fold(
                        user  -> ResponseEntity.ok(UserResponse.fromUser(user)),
                        error -> ResponseEntity.status(toHttpStatus(error))
                                               .body(Map.of("error", error.message()))
                );
    }

    /**
     * PUT /api/users/result/{id}
     *
     * Demonstrates: map for pure transformation + fold
     *
     * The service uses map (not flatMap) for the save step because once we
     * have a valid User entity the repository.save() cannot produce a UserError.
     * map is the right choice when the transformation is total (never fails).
     *
     * Try it:
     *   PUT /api/users/result/1   { "name": "Alice Updated", "maxAlerts": 5 }  → 200
     *   PUT /api/users/result/1   { "maxAlerts": -1 }                          → 400 ValidationError
     *   PUT /api/users/result/9   { "name": "Ghost" }                          → 404 NotFound
     */
    @PutMapping("/result/{id}")
    public ResponseEntity<?> resultUpdateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        User updateData = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .notificationChannel(request.getNotificationChannel())
                .timezone(request.getTimezone())
                .maxAlerts(request.getMaxAlerts() != null ? request.getMaxAlerts() : 0)
                .build();

        return userService.resultUpdateUser(id, updateData)
                .fold(
                        updated -> ResponseEntity.ok(UserResponse.fromUser(updated)),
                        error   -> ResponseEntity.status(toHttpStatus(error))
                                                 .body(Map.of("error", error.message()))
                );
    }

    /**
     * GET /api/users/result/{id}/quota
     *
     * Demonstrates: recover — provide a fallback when a specific error occurs
     *
     * If the user has exceeded their quota we recover with a 200 response that
     * explains the situation, rather than a 4xx. This shows how recover lets
     * you change strategy mid-chain without unwrapping the Result manually.
     *
     * recover receives the error and returns a new Result — it can choose to:
     *   • succeed with a fallback value  (recovered)
     *   • fail with a different error    (re-routed)
     *
     * Try it:
     *   GET /api/users/result/1/quota  (user with slots free)    → 200 { canCreate: true }
     *   GET /api/users/result/1/quota  (user at quota limit)     → 200 { canCreate: false, reason: "..." }
     *   GET /api/users/result/9/quota  (user not found)          → 404 (recover only handles QuotaExceeded)
     */
    @GetMapping("/result/{id}/quota")
    public ResponseEntity<?> resultCheckQuota(@PathVariable Long id) {
        return userService.resultValidateAlertQuota(id)
                // recover intercepts QuotaExceeded specifically; other errors propagate
                .recover(error -> switch (error) {
                    case UserError.QuotaExceeded q ->
                            // Soft recovery: turn "quota exceeded" into a successful response
                            // that carries a canCreate=false flag instead of an error status
                            Result.success(null); // signal: recovered, will fold as success below
                    default -> Result.failure(error); // all other errors stay failures
                })
                // mapError converts remaining UserError failures into a response map
                // so fold's onError branch gets a uniform Map<String,Object>
                .mapError(error -> Map.of("error", error.message(), "status", toHttpStatus(error).value()))
                .fold(
                        ignored -> {
                            // We recovered — re-derive the readable slot info
                            int remaining = userService.getRemainingAlertSlots(id);
                            boolean canCreate = remaining > 0;
                            return ResponseEntity.ok(Map.of(
                                    "userId", id,
                                    "canCreate", canCreate,
                                    "remainingSlots", remaining
                            ));
                        },
                        errMap  -> ResponseEntity.status((int) errMap.get("status"))
                                                 .body(errMap)
                );
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Maps each UserError variant to its canonical HTTP status code.
     *
     * Because UserError is sealed, the compiler verifies this switch is
     * exhaustive — if a new variant is added the code won't compile until
     * this mapping is updated. No silent fall-throughs.
     */
    private HttpStatus toHttpStatus(UserError error) {
        return switch (error) {
            case UserError.NotFound       ignored -> HttpStatus.NOT_FOUND;            // 404
            case UserError.DuplicateEmail ignored -> HttpStatus.CONFLICT;             // 409
            case UserError.QuotaExceeded  ignored -> HttpStatus.UNPROCESSABLE_ENTITY; // 422
            case UserError.ValidationError ignored -> HttpStatus.BAD_REQUEST;          // 400
            case UserError.InactiveAccount ignored -> HttpStatus.FORBIDDEN;            // 403
        };
    }
}