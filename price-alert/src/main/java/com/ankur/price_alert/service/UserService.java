package com.ankur.price_alert.service;

import com.ankur.price_alert.exception.AlertQuotaExceededException;
import com.ankur.price_alert.exception.DuplicateResourceException;
import com.ankur.price_alert.exception.UserNotFoundException;
import com.ankur.price_alert.model.NotificationChannel;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.UserRepository;
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
}