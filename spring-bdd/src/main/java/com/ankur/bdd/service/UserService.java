package com.ankur.bdd.service;

import com.ankur.bdd.exception.AlertQuotaExceededException;
import com.ankur.bdd.exception.DuplicateResourceException;
import com.ankur.bdd.exception.UserNotFoundException;
import com.ankur.bdd.model.NotificationChannel;
import com.ankur.bdd.model.User;
import com.ankur.bdd.repository.UserRepository;
import com.ankur.bdd.result.Result;
import com.ankur.bdd.result.UserError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
        if (updatedUser.getName() != null)                existingUser.setName(updatedUser.getName());
        if (updatedUser.getPhone() != null)               existingUser.setPhone(updatedUser.getPhone());
        if (updatedUser.getTimezone() != null)            existingUser.setTimezone(updatedUser.getTimezone());
        if (updatedUser.getNotificationChannel() != null) existingUser.setNotificationChannel(updatedUser.getNotificationChannel());
        if (updatedUser.getMaxAlerts() > 0)               existingUser.setMaxAlerts(updatedUser.getMaxAlerts());
        return userRepository.save(existingUser);
    }

    public User updateNotificationChannel(Long userId, NotificationChannel channel) {
        User user = getById(userId);
        user.setNotificationChannel(channel);
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
        return user.getAlerts().size() < user.getMaxAlerts();
    }

    @Transactional(readOnly = true)
    public void validateAlertQuota(Long userId) {
        User user = getById(userId);
        int count = user.getAlerts().size();
        if (count >= user.getMaxAlerts()) {
            throw new AlertQuotaExceededException(userId, count, user.getMaxAlerts());
        }
    }

    public User getOrCreateUser(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name));
    }

    // ==================== RESULT-BASED API ====================

    public Result<User, UserError> resultCreateUser(User user) {
        return Result.<String, UserError>success(user.getEmail())
                .flatMap(email -> {
                    if (email == null || email.isBlank())
                        return Result.failure(new UserError.ValidationError("email", "must not be blank"));
                    return Result.success(email);
                })
                .flatMap(email -> {
                    if (userRepository.existsByEmail(email))
                        return Result.failure(new UserError.DuplicateEmail(email));
                    return Result.success(email);
                })
                .map(email -> userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Result<User, UserError> resultGetById(Long id) {
        return userRepository.findById(id)
                .<Result<User, UserError>>map(Result::success)
                .orElse(Result.failure(new UserError.NotFound(String.valueOf(id))));
    }

    @Transactional(readOnly = true)
    public Result<User, UserError> resultGetByEmail(String email) {
        return userRepository.findByEmail(email)
                .<Result<User, UserError>>map(Result::success)
                .orElse(Result.failure(new UserError.NotFound(email)));
    }

    @Transactional(readOnly = true)
    public Result<User, UserError> resultGetActiveById(Long id) {
        return resultGetById(id)
                .flatMap(user -> user.isActive()
                        ? Result.success(user)
                        : Result.failure(new UserError.InactiveAccount(id)));
    }

    public Result<User, UserError> resultUpdateUser(Long id, User updateData) {
        if (updateData.getMaxAlerts() < 0)
            return Result.failure(new UserError.ValidationError("maxAlerts", "must be >= 0"));
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

    @Transactional(readOnly = true)
    public Result<Void, UserError> resultValidateAlertQuota(Long userId) {
        return resultGetById(userId)
                .flatMap(user -> {
                    int count = user.getAlerts().size();
                    if (count >= user.getMaxAlerts())
                        return Result.failure(new UserError.QuotaExceeded(userId, count, user.getMaxAlerts()));
                    return Result.success(null);
                });
    }
}