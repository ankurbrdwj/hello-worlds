package com.ankur.bdd.dto;

/**
 * Maps to POST /api/users in the price-alert service.
 */
public record UserRequest(String email, String name) {
}