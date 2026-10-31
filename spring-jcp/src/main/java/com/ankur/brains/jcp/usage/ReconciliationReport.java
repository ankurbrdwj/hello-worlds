package com.ankur.brains.jcp.usage;

import java.util.List;

public final class ReconciliationReport {

	private final List<Drift> drifted;

	ReconciliationReport(List<Drift> drifted) {
		this.drifted = drifted;
	}

	public List<Drift> getDrifted() {
		return drifted;
	}

}
