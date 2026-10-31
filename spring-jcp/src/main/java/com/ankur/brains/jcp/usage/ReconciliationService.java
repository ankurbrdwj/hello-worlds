package com.ankur.brains.jcp.usage;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReconciliationService {

	private final JdbcTemplate jdbc;
	private final TransactionTemplate transaction;

	ReconciliationService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
		this.jdbc = jdbc;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	public ReconciliationReport reconcile(boolean repair) {
		List<Drift> candidates = findDriftSnapshot();
		if (!repair) {
			return new ReconciliationReport(candidates);
		}
		List<Drift> repaired = new ArrayList<>();
		for (Drift candidate : candidates) {
			Drift fixed = repairOne(candidate.getCustomerId(), candidate.getFeature());
			if (fixed != null) {
				repaired.add(fixed);
			}
		}
		return new ReconciliationReport(repaired);
	}

	/** One consistent snapshot of committed data; cheap and lock-free, but may already be stale. */
	private List<Drift> findDriftSnapshot() {
		return jdbc.query(
				"SELECT e.customer_id, e.feature, e.used AS recorded_used, COALESCE(SUM(u.units), 0) AS actual_used"
						+ " FROM entitlement e"
						+ " LEFT JOIN usage_event u ON u.customer_id = e.customer_id AND u.feature = e.feature"
						+ " GROUP BY e.customer_id, e.feature, e.used"
						+ " HAVING e.used <> COALESCE(SUM(u.units), 0)",
				(rs, row) -> new Drift(rs.getString("customer_id"), rs.getString("feature"),
						rs.getLong("recorded_used"), rs.getLong("actual_used")));
	}

	/**
	 * Locks the counter row first and only then sums the events. An ingest that was in flight when the snapshot
	 * was taken has either committed by the time the lock is granted (its event is then in the sum) or is still
	 * blocked behind us (its increment is then applied on top of the repaired value). Either way nothing is lost.
	 */
	private Drift repairOne(String customerId, String feature) {
		return transaction.execute(status -> {
			long recorded = jdbc.queryForObject(
					"SELECT used FROM entitlement WHERE customer_id = ? AND feature = ? FOR UPDATE",
					Long.class, customerId, feature);
			long actual = jdbc.queryForObject(
					"SELECT COALESCE(SUM(units), 0) FROM usage_event WHERE customer_id = ? AND feature = ?",
					Long.class, customerId, feature);
			if (recorded == actual) {
				return null;
			}
			jdbc.update("UPDATE entitlement SET used = ? WHERE customer_id = ? AND feature = ?",
					actual, customerId, feature);
			return new Drift(customerId, feature, recorded, actual);
		});
	}

}
