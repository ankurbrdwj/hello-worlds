package com.ankur.brains.jcp.usage;

/** Response body for an accepted or replayed usage event. */
public final class UsageReceipt {

	private final String eventKey;
	private final Long usedAfter;

	UsageReceipt(String eventKey, Long usedAfter) {
		this.eventKey = eventKey;
		this.usedAfter = usedAfter;
	}

	public String getEventKey() {
		return eventKey;
	}

	public Long getUsedAfter() {
		return usedAfter;
	}

}
