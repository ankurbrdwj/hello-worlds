package com.ankur.brains.jcp.usage;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class UsageEvent {

	private final String eventKey;
	private final String customerId;
	private final String userId;
	private final String feature;
	private final long units;
	private final Instant occurredAt;

	@JsonCreator
	UsageEvent(@JsonProperty("eventKey") String eventKey,
			@JsonProperty("customerId") String customerId,
			@JsonProperty("userId") String userId,
			@JsonProperty("feature") String feature,
			@JsonProperty("units") long units,
			@JsonProperty("occurredAt") Instant occurredAt) {
		this.eventKey = eventKey;
		this.customerId = customerId;
		this.userId = userId;
		this.feature = feature;
		this.units = units;
		this.occurredAt = occurredAt;
	}

	public String getEventKey() {
		return eventKey;
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getUserId() {
		return userId;
	}

	public String getFeature() {
		return feature;
	}

	public long getUnits() {
		return units;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

}
