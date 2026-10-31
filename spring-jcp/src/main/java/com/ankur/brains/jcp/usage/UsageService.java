package com.ankur.brains.jcp.usage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class UsageService {

	private final JdbcTemplate jdbc;

	UsageService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	// Must be public: Spring's proxy-based @Transactional silently ignores non-public methods.
	@Transactional
	public UsageOutcome record(UsageEvent event) {
		int inserted = jdbc.update("INSERT INTO usage_event (event_key, customer_id, user_id, feature, units, occurred_at)"
				+ " VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (event_key) DO NOTHING",
				event.getEventKey(), event.getCustomerId(), event.getUserId(),
				event.getFeature(), event.getUnits(), Timestamp.from(event.getOccurredAt()));
		if (inserted == 0) {
			return replay(event);
		}

		// One atomic statement: Postgres locks the entitlement row, so concurrent requests are serialized and
		// the check cannot be raced. "<=" lets usage reach the quota exactly; only strictly over is rejected.
		List<Long> usedAfter = jdbc.queryForList("UPDATE entitlement SET used = used + ?"
				+ " WHERE customer_id = ? AND feature = ? AND used + ? <= quota RETURNING used", Long.class,
				event.getUnits(), event.getCustomerId(), event.getFeature(), event.getUnits());
		if (usedAfter.isEmpty()) {
			// undo the event insert so the key is not burned by a rejected request
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return new UsageOutcome(UsageResult.QUOTA_EXCEEDED, null);
		}
		jdbc.update("UPDATE usage_event SET used_after = ? WHERE event_key = ?", usedAfter.get(0), event.getEventKey());
		return new UsageOutcome(UsageResult.RECORDED, usedAfter.get(0));
	}

	private UsageOutcome replay(UsageEvent event) {
		Map<String, Object> stored = jdbc.queryForMap(
				"SELECT customer_id, user_id, feature, units, occurred_at, used_after FROM usage_event"
						+ " WHERE event_key = ?", event.getEventKey());
		boolean same = stored.get("customer_id").equals(event.getCustomerId())
				&& stored.get("user_id").equals(event.getUserId())
				&& stored.get("feature").equals(event.getFeature())
				&& ((Number) stored.get("units")).longValue() == event.getUnits()
				&& ((Timestamp) stored.get("occurred_at")).toInstant().equals(event.getOccurredAt());
		Number usedAfter = (Number) stored.get("used_after");
		return new UsageOutcome(same ? UsageResult.DUPLICATE : UsageResult.KEY_REUSED_WITH_DIFFERENT_CONTENT,
				same && usedAfter != null ? usedAfter.longValue() : null);
	}

}
