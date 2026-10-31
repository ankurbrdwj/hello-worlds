package com.ankur.brains.jcp.usage;

/** One entitlement whose counter disagrees with the sum of its recorded events. */
public final class Drift {

	private final String customerId;
	private final String feature;
	private final long recordedUsed;
	private final long actualUsed;

	Drift(String customerId, String feature, long recordedUsed, long actualUsed) {
		this.customerId = customerId;
		this.feature = feature;
		this.recordedUsed = recordedUsed;
		this.actualUsed = actualUsed;
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getFeature() {
		return feature;
	}

	/** What the entitlement counter says. */
	public long getRecordedUsed() {
		return recordedUsed;
	}

	/** What the recorded events add up to; this is the source of truth. */
	public long getActualUsed() {
		return actualUsed;
	}

}
