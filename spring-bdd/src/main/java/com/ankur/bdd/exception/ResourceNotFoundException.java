package com.ankur.bdd.exception;

public abstract class ResourceNotFoundException extends PriceAlertException {

    private final String resourceType;
    private final Object resourceId;

    protected ResourceNotFoundException(String resourceType, Object resourceId, String errorCode) {
        super(String.format("%s not found with id: %s", resourceType, resourceId), errorCode);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public Object getResourceId() { return resourceId; }
}