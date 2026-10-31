package com.ankur.brains.jcp.usage;

public final class UsageOutcome {

	private final UsageResult result;
	private final Long usedAfter;

	UsageOutcome(UsageResult result, Long usedAfter) {
		this.result = result;
		this.usedAfter = usedAfter;
	}

	public UsageResult getResult() {
		return result;
	}

	/** Running total right after the original event was counted; null when nothing was counted. */
	public Long getUsedAfter() {
		return usedAfter;
	}

}
