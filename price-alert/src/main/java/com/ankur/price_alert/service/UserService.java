package com.ankur.price_alert.service;

import com.ankur.price_alert.exception.AlertQuotaExceededException;
import com.ankur.price_alert.exception.DuplicateResourceException;
import com.ankur.price_alert.exception.UserNotFoundException;
import com.ankur.price_alert.model.NotificationChannel;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.UserRepository;
import com.ankur.price_alert.result.Result;
import com.ankur.price_alert.result.UserError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for user management.
 * Uses custom exception hierarchy for better error handling.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ==================== CREATE ====================

    public User createUser(String email, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", email);
        }

        User user = User.builder()
                .email(email)
                .name(name)
                .build();

        return userRepository.save(user);
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", user.getEmail());
        }
        return userRepository.save(user);
    }

    // ==================== READ ====================

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ==================== UPDATE ====================

    public User updateUser(Long id, User updatedUser) {
        User existingUser = getById(id);

        if (updatedUser.getName() != null) {
            existingUser.setName(updatedUser.getName());
        }
        if (updatedUser.getPhone() != null) {
            existingUser.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getTimezone() != null) {
            existingUser.setTimezone(updatedUser.getTimezone());
        }
        if (updatedUser.getNotificationChannel() != null) {
            existingUser.setNotificationChannel(updatedUser.getNotificationChannel());
        }
        if (updatedUser.getMaxAlerts() > 0) {
            existingUser.setMaxAlerts(updatedUser.getMaxAlerts());
        }

        return userRepository.save(existingUser);
    }

    public User updateNotificationChannel(Long userId, NotificationChannel channel) {
        User user = getById(userId);
        user.setNotificationChannel(channel);
        return userRepository.save(user);
    }

    public User updatePhone(Long userId, String phone) {
        User user = getById(userId);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public User activateUser(Long userId) {
        User user = getById(userId);
        user.setActive(true);
        return userRepository.save(user);
    }

    public User deactivateUser(Long userId) {
        User user = getById(userId);
        user.setActive(false);
        return userRepository.save(user);
    }

    // ==================== DELETE ====================

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    public void deleteByEmail(String email) {
        User user = getByEmail(email);
        userRepository.delete(user);
    }

    // ==================== BUSINESS LOGIC ====================

    @Transactional(readOnly = true)
    public boolean canCreateMoreAlerts(Long userId) {
        User user = getById(userId);
        int currentAlertCount = user.getAlerts().size();
        return currentAlertCount < user.getMaxAlerts();
    }

    /**
     * Validates that user can create more alerts, throws exception if quota exceeded.
     *
     * @param userId the user ID
     * @throws AlertQuotaExceededException if user has reached their alert limit
     */
    @Transactional(readOnly = true)
    public void validateAlertQuota(Long userId) {
        User user = getById(userId);
        int currentAlertCount = user.getAlerts().size();
        if (currentAlertCount >= user.getMaxAlerts()) {
            throw new AlertQuotaExceededException(userId, currentAlertCount, user.getMaxAlerts());
        }
    }

    @Transactional(readOnly = true)
    public int getRemainingAlertSlots(Long userId) {
        User user = getById(userId);
        int currentAlertCount = user.getAlerts().size();
        return Math.max(0, user.getMaxAlerts() - currentAlertCount);
    }

    public User getOrCreateUser(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name));
    }

    // ==================== RESULT-BASED API ====================
    // These methods return Result<T, UserError> instead of throwing exceptions.
    // Errors are values — callers compose them with map / flatMap / fold.

    /**
     * Creates a user after running a two-step validation chain with flatMap.
     *
     * Step 1: validate the email field (ValidationError on blank)
     * Step 2: check uniqueness   (DuplicateEmail on conflict)
     * Step 3: persist and wrap   (map — pure transformation)
     *
     * If any step returns Failure, the chain short-circuits and subsequent
     * flatMap / map calls are skipped — exactly like a railway switch.
     */
    public Result<User, UserError> resultCreateUser(User user) {
        return Result.<String, UserError>success(user.getEmail())
                // Step 1 — field validation
                .flatMap(email -> {
                    if (email == null || email.isBlank()) {
                        return Result.failure(new UserError.ValidationError("email", "must not be blank"));
                    }
                    return Result.success(email);
                })
                // Step 2 — uniqueness check
                .flatMap(email -> {
                    if (userRepository.existsByEmail(email)) {
                        return Result.failure(new UserError.DuplicateEmail(email));
                    }
                    return Result.success(email);
                })
                // Step 3 — persist (map because save() cannot itself fail with a UserError)
                .map(email -> userRepository.save(user));
    }

    /**
     * Looks up a user by id; returns NotFound instead of throwing.
     */
    @Transactional(readOnly = true)
    public Result<User, UserError> resultGetById(Long id) {
        return userRepository.findById(id)
                .<Result<User, UserError>>map(Result::success)
                .orElse(Result.failure(new UserError.NotFound(String.valueOf(id))));
    }

    /**
     * Looks up a user by email; returns NotFound instead of throwing.
     */
    @Transactional(readOnly = true)
    public Result<User, UserError> resultGetByEmail(String email) {
        return userRepository.findByEmail(email)
                .<Result<User, UserError>>map(Result::success)
                .orElse(Result.failure(new UserError.NotFound(email)));
    }

    /**
     * Fetches a user AND asserts they are active — two checks chained with flatMap.
     *
     * flatMap is the right tool here: the second check (active?) depends on the
     * result of the first (found?), and each step can independently fail.
     */
    @Transactional(readOnly = true)
    public Result<User, UserError> resultGetActiveById(Long id) {
        return resultGetById(id)
                .flatMap(user -> user.isActive()
                        ? Result.success(user)
                        : Result.failure(new UserError.InactiveAccount(id)));
    }

    /**
     * Updates a user after validating maxAlerts with map.
     *
     * map is used for the final save: once we have a valid, found user the
     * update is a pure transformation — it cannot produce a UserError itself.
     */
    public Result<User, UserError> resultUpdateUser(Long id, User updateData) {
        if (updateData.getMaxAlerts() < 0) {
            return Result.failure(new UserError.ValidationError("maxAlerts", "must be >= 0"));
        }
        return resultGetById(id)
                .map(existing -> {
                    if (updateData.getName() != null)                existing.setName(updateData.getName());
                    if (updateData.getPhone() != null)               existing.setPhone(updateData.getPhone());
                    if (updateData.getTimezone() != null)            existing.setTimezone(updateData.getTimezone());
                    if (updateData.getNotificationChannel() != null) existing.setNotificationChannel(updateData.getNotificationChannel());
                    if (updateData.getMaxAlerts() > 0)               existing.setMaxAlerts(updateData.getMaxAlerts());
                    return userRepository.save(existing);
                });
    }

    /**
     * Validates alert quota; returns QuotaExceeded instead of throwing.
     * Returns Result<Void, UserError> — success carries no meaningful value.
     */
    @Transactional(readOnly = true)
    public Result<Void, UserError> resultValidateAlertQuota(Long userId) {
        return resultGetById(userId)
                .flatMap(user -> {
                    int count = user.getAlerts().size();
                    if (count >= user.getMaxAlerts()) {
                        return Result.failure(new UserError.QuotaExceeded(userId, count, user.getMaxAlerts()));
                    }
                    return Result.success(null);
                });
    }
}