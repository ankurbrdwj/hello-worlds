package com.ankur.ratelimiter.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a client being rate limited.
 * Clients can be identified by user ID, IP address, or API key.
 */
public class Client {

    public enum ClientType {
        USER_ID,
        IP_ADDRESS,
        API_KEY
    }

    private String id;
    private String identifier;
    private ClientType type;
    private List<RateLimitRule> rateLimitRules;

    public Client() {
        this.rateLimitRules = new ArrayList<>();
    }

    public Client(String id, String identifier, ClientType type) {
        this.id = id;
        this.identifier = identifier;
        this.type = type;
        this.rateLimitRules = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public ClientType getType() {
        return type;
    }

    public void setType(ClientType type) {
        this.type = type;
    }

    public List<RateLimitRule> getRateLimitRules() {
        return rateLimitRules;
    }

    public void setRateLimitRules(List<RateLimitRule> rateLimitRules) {
        this.rateLimitRules = rateLimitRules;
    }

    public void addRateLimitRule(RateLimitRule rule) {
        this.rateLimitRules.add(rule);
    }

    @Override
    public String toString() {
        return "Client{" +
                "id='" + id + '\'' +
                ", identifier='" + identifier + '\'' +
                ", type=" + type +
                ", rateLimitRules=" + rateLimitRules.size() +
                '}';
    }
}