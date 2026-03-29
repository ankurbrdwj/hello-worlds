package com.ankur.bdd.dto;

/**
 * Maps to the response from POST/GET /api/users in the price-alert service.
 */
public record UserResponse(Long id, String email, String name, boolean active) {
}