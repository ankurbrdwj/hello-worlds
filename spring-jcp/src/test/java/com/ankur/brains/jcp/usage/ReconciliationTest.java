package com.ankur.brains.jcp.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.ankur.brains.jcp.PostgresIntegrationTest;

@AutoConfigureMockMvc
class ReconciliationTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Test
	void dryRunReportsDriftWithoutChangingTheCounter() throws Exception {
		grantQuota("dry-run", 100);
		postEvent("dry-run", "rc-dry-1", 3).andExpect(status().isCreated());
		setUsed("dry-run", 99); // corrupt the counter: events say 3, counter says 99
		try {
			mockMvc.perform(post("/v1/admin/reconciliations").param("repair", "false"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.drifted[?(@.customerId=='dry-run')].recordedUsed").value(99))
					.andExpect(jsonPath("$.drifted[?(@.customerId=='dry-run')].actualUsed").value(3));

			assertThat(usedBy("dry-run")).isEqualTo(99L);
		} finally {
			setUsed("dry-run", 3); // do not leave drift behind for other tests
		}
	}

	@Test
	void repairFixesTheCounterAndASecondRunFindsNothing() throws Exception {
		grantQuota("repair", 100);
		postEvent("repair", "rc-repair-1", 3).andExpect(status().isCreated());
		setUsed("repair", 99);

		mockMvc.perform(post("/v1/admin/reconciliations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.drifted[?(@.customerId=='repair')].actualUsed").value(3));

		assertThat(usedBy("repair")).isEqualTo(3L);
		mockMvc.perform(post("/v1/admin/reconciliations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.drifted[?(@.customerId=='repair')]").isEmpty());
	}

	@Test
	void repairAlsoRaisesACounterThatLostAnUpdate() throws Exception {
		grantQuota("lost-update", 100);
		postEvent("lost-update", "rc-lost-1", 3).andExpect(status().isCreated());
		setUsed("lost-update", 0); // counter too LOW: events say 3, counter says 0

		mockMvc.perform(post("/v1/admin/reconciliations"))
				.andExpect(jsonPath("$.drifted[?(@.customerId=='lost-update')].recordedUsed").value(0))
				.andExpect(jsonPath("$.drifted[?(@.customerId=='lost-update')].actualUsed").value(3));

		assertThat(usedBy("lost-update")).isEqualTo(3L);
	}

	@Test
	void repairDoesNotLoseAnEventCommittedWhileReconciliationWaitsOnTheRow() throws Exception {
		grantQuota("live", 100);
		setUsed("live", 50); // real drift: no events, counter says 50

		CountDownLatch applied = new CountDownLatch(1);
		CountDownLatch commit = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			// An in-flight ingest: event inserted and counter bumped, but not yet committed.
			Future<?> ingest = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
				jdbc.update("INSERT INTO usage_event (event_key, customer_id, user_id, feature, units, occurred_at,"
						+ " used_after) VALUES ('rc-live-1', 'live', 'dev42', 'ai-credits', 3, now(), 53)");
				jdbc.update("UPDATE entitlement SET used = used + 3 WHERE customer_id = 'live'");
				applied.countDown();
				await(commit);
				return null;
			}));
			assertThat(applied.await(10, TimeUnit.SECONDS)).isTrue();

			// Reconcile sees only committed data (used=50, events=0) and must wait for the row lock.
			Future<Integer> reconcile = pool.submit(() -> mockMvc.perform(post("/v1/admin/reconciliations"))
					.andReturn().getResponse().getStatus());
			awaitSomeoneBlockedOnALock();

			commit.countDown();
			ingest.get(10, TimeUnit.SECONDS);
			assertThat(reconcile.get(10, TimeUnit.SECONDS)).isEqualTo(200);
		} finally {
			pool.shutdownNow();
		}

		// the committed event (3 units) must be reflected in the counter
		assertThat(usedBy("live")).isEqualTo(3L);
		mockMvc.perform(post("/v1/admin/reconciliations").param("repair", "false"))
				.andExpect(jsonPath("$.drifted[?(@.customerId=='live')]").isEmpty());
	}

	private void await(CountDownLatch latch) {
		try {
			latch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void awaitSomeoneBlockedOnALock() throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			Integer waiting = jdbc.queryForObject(
					"SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'", Integer.class);
			if (waiting != null && waiting > 0) {
				return;
			}
			Thread.sleep(50);
		}
		throw new AssertionError("reconciliation never blocked on the row lock");
	}

	private void grantQuota(String customerId, long quota) {
		jdbc.update("INSERT INTO entitlement (customer_id, feature, quota) VALUES (?, 'ai-credits', ?)",
				customerId, quota);
	}

	private void setUsed(String customerId, long used) {
		jdbc.update("UPDATE entitlement SET used = ? WHERE customer_id = ? AND feature = 'ai-credits'",
				used, customerId);
	}

	private long usedBy(String customerId) {
		return jdbc.queryForObject("SELECT used FROM entitlement WHERE customer_id = ? AND feature = 'ai-credits'",
				Long.class, customerId);
	}

	private ResultActions postEvent(String customerId, String eventKey, long units) throws Exception {
		String body = "{\"eventKey\": \"" + eventKey + "\", \"customerId\": \"" + customerId + "\","
				+ " \"userId\": \"dev42\", \"feature\": \"ai-credits\", \"units\": " + units
				+ ", \"occurredAt\": \"2026-10-05T10:01:00Z\"}";
		return mockMvc.perform(post("/v1/usage-events").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
