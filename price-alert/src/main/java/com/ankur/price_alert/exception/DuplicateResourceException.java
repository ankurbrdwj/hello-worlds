package com.ankur.price_alert.exception;

/**
 * Exception thrown when attempting to create a duplicate resource.
 */
public class DuplicateResourceException extends PriceAlertException {

    public static final String ERROR_CODE = "DUPLICATE_RESOURCE";

    private final String resourceType;
    private final String identifier;

    public DuplicateResourceException(String resourceType, String identifier) {
        super(String.format("%s already exists with identifier: %s", resourceType, identifier), ERROR_CODE);
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
