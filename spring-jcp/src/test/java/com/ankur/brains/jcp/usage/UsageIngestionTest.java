package com.ankur.brains.jcp.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.ankur.brains.jcp.PostgresIntegrationTest;
import com.ankur.brains.jcp.SpringJcpApplication;

@AutoConfigureMockMvc
class UsageIngestionTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void usageEqualToQuotaIsAcceptedAndOneUnitOverIsConflict() throws Exception {
		grantQuota("quota-boundary", 10);

		postEvent("quota-boundary", "qb-1", 10).andExpect(status().isCreated());

		postEvent("quota-boundary", "qb-2", 1).andExpect(status().isConflict());
	}

	@Test
	void duplicateEventKeyIsAcceptedOnceAndReplayedAsOk() throws Exception {
		grantQuota("duplicate-key", 100);

		postEvent("duplicate-key", "dk-1", 3).andExpect(status().isCreated());

		postEvent("duplicate-key", "dk-1", 3).andExpect(status().isOk());
	}

	@Test
	void acceptedEventIsPersistedAndCounted() throws Exception {
		grantQuota("persisted", 100);

		postEvent("persisted", "p-1", 3).andExpect(status().isCreated());

		assertThat(usedBy("persisted")).isEqualTo(3L);
		assertThat(storedEventCount("p-1")).isEqualTo(1);
	}

	@Test
	void sameEventKeySentConcurrentlyIsRecordedExactlyOnce() throws Exception {
		grantQuota("concurrent-dup", 100);

		List<Integer> statuses = inParallel(16, i -> statusOf(postEvent("concurrent-dup", "cd-1", 3)));

		assertThat(statuses).containsOnly(201, 200);
		assertThat(Collections.frequency(statuses, 201)).isEqualTo(1);
		assertThat(usedBy("concurrent-dup")).isEqualTo(3L);
		assertThat(storedEventCount("cd-1")).isEqualTo(1);
	}

	@Test
	void parallelDistinctEventsNeverExceedTheQuota() throws Exception {
		grantQuota("concurrent-quota", 10);

		List<Integer> statuses = inParallel(30, i -> statusOf(postEvent("concurrent-quota", "cq-" + i, 1)));

		assertThat(Collections.frequency(statuses, 201)).isEqualTo(10);
		assertThat(Collections.frequency(statuses, 409)).isEqualTo(20);
		assertThat(usedBy("concurrent-quota")).isEqualTo(10L);
	}

	@Test
	void secondInstanceOrRestartedInstanceDoesNotCountAnAlreadyRecordedEvent() throws Exception {
		grantQuota("two-instances", 100);
		postEvent("two-instances", "ti-1", 3).andExpect(status().isCreated());

		// A brand-new application context shares nothing with the first one except the database,
		// which is exactly what a second instance, or this instance after a restart, looks like.
		try (ConfigurableApplicationContext other = new SpringApplicationBuilder(SpringJcpApplication.class)
				.web(WebApplicationType.NONE)
				.properties("spring.datasource.url=" + POSTGRES.getJdbcUrl(),
						"spring.datasource.username=" + POSTGRES.getUsername(),
						"spring.datasource.password=" + POSTGRES.getPassword())
				.run()) {
			UsageEvent resent = new UsageEvent("ti-1", "two-instances", "dev42", "ai-credits", 3,
					Instant.parse("2026-10-05T10:01:00Z"));

			assertThat(other.getBean(UsageService.class).record(resent).getResult()).isEqualTo(UsageResult.DUPLICATE);
		}

		assertThat(usedBy("two-instances")).isEqualTo(3L);
	}

	@Test
	void sameEventKeyWithDifferentContentIsRejectedAsUnprocessable() throws Exception {
		grantQuota("key-reuse", 100);
		postEvent("key-reuse", "kr-1", 3).andExpect(status().isCreated());

		postEvent("key-reuse", "kr-1", 5).andExpect(status().isUnprocessableEntity());

		assertThat(usedBy("key-reuse")).isEqualTo(3L);
	}

	@Test
	void duplicateReplaysTheOriginalResultNotTheCurrentTotal() throws Exception {
		grantQuota("replay", 100);

		postEvent("replay", "rp-1", 3).andExpect(status().isCreated()).andExpect(jsonPath("$.usedAfter").value(3));
		postEvent("replay", "rp-2", 4).andExpect(status().isCreated()).andExpect(jsonPath("$.usedAfter").value(7));

		postEvent("replay", "rp-1", 3).andExpect(status().isOk())
				.andExpect(jsonPath("$.eventKey").value("rp-1"))
				.andExpect(jsonPath("$.usedAfter").value(3));
	}

	@Test
	void failureWhileCountingLeavesNoHalfWrittenEvent() throws Exception {
		grantQuota("half-write", 100);
		// A trigger makes the counter update blow up AFTER the event row was inserted.
		jdbc.execute("CREATE OR REPLACE FUNCTION fail_for_half_write() RETURNS trigger AS $$ BEGIN"
				+ " IF NEW.customer_id = 'half-write' THEN RAISE EXCEPTION 'injected failure'; END IF;"
				+ " RETURN NEW; END; $$ LANGUAGE plpgsql");
		jdbc.execute("CREATE TRIGGER fail_half_write BEFORE UPDATE ON entitlement"
				+ " FOR EACH ROW EXECUTE FUNCTION fail_for_half_write()");
		try {
			assertThat(statusOf(postEventLenient("half-write", "hw-1", 3))).isEqualTo(-1);
		} finally {
			jdbc.execute("DROP TRIGGER fail_half_write ON entitlement");
		}

		assertThat(storedEventCount("hw-1")).isEqualTo(0);
		assertThat(usedBy("half-write")).isEqualTo(0L);
		// the key was not burned: the same event succeeds once the failure is gone
		postEvent("half-write", "hw-1", 3).andExpect(status().isCreated());
	}

	@Test
	void eventsArrivingOutOfOrderGiveTheSameTotalAsInOrder() throws Exception {
		grantQuota("in-order", 100);
		grantQuota("out-of-order", 100);

		postEvent("in-order", "io-1", 1, "2026-10-05T10:01:00Z").andExpect(status().isCreated());
		postEvent("in-order", "io-2", 2, "2026-10-05T10:03:00Z").andExpect(status().isCreated());
		postEvent("in-order", "io-3", 3, "2026-10-05T10:05:00Z").andExpect(status().isCreated());

		postEvent("out-of-order", "oo-3", 3, "2026-10-05T10:05:00Z").andExpect(status().isCreated());
		postEvent("out-of-order", "oo-1", 1, "2026-10-05T10:01:00Z").andExpect(status().isCreated());
		postEvent("out-of-order", "oo-2", 2, "2026-10-05T10:03:00Z").andExpect(status().isCreated());

		assertThat(usedBy("out-of-order")).isEqualTo(usedBy("in-order")).isEqualTo(6L);
	}

	@Test
	void failureAfterTheCounterWasIncrementedRollsBackTheCounterToo() throws Exception {
		grantQuota("late-failure", 100);
		// This trigger fires on the LAST statement (storing used_after), so the counter increment
		// and the event insert have both already been applied inside the transaction.
		jdbc.execute("CREATE OR REPLACE FUNCTION fail_for_late_failure() RETURNS trigger AS $$ BEGIN"
				+ " IF NEW.customer_id = 'late-failure' THEN RAISE EXCEPTION 'injected late failure'; END IF;"
				+ " RETURN NEW; END; $$ LANGUAGE plpgsql");
		jdbc.execute("CREATE TRIGGER fail_late_failure BEFORE UPDATE ON usage_event"
				+ " FOR EACH ROW EXECUTE FUNCTION fail_for_late_failure()");
		try {
			assertThat(statusOf(postEventLenient("late-failure", "lf-1", 3))).isEqualTo(-1);
		} finally {
			jdbc.execute("DROP TRIGGER fail_late_failure ON usage_event");
		}

		assertThat(usedBy("late-failure")).isEqualTo(0L);
		assertThat(storedEventCount("lf-1")).isEqualTo(0);
		postEvent("late-failure", "lf-1", 3).andExpect(status().isCreated());
	}

	/** Runs the task on n threads released at the same instant; a thrown exception is reported as status -1. */
	private List<Integer> inParallel(int n, ThrowingTask task) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(n);
		CountDownLatch go = new CountDownLatch(1);
		try {
			List<Future<Integer>> futures = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				final int index = i;
				futures.add(pool.submit(() -> {
					go.await();
					try {
						return task.run(index);
					} catch (Exception e) {
						return -1;
					}
				}));
			}
			go.countDown();
			List<Integer> results = new ArrayList<>();
			for (Future<Integer> f : futures) {
				results.add(f.get(30, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			pool.shutdownNow();
		}
	}

	private interface ThrowingTask {
		int run(int index) throws Exception;
	}

	/** Like postEvent, but a server-side exception is reported as status -1 instead of being thrown. */
	private ResultActions postEventLenient(String customerId, String eventKey, long units) {
		try {
			return postEvent(customerId, eventKey, units);
		} catch (Exception e) {
			return null;
		}
	}

	private int statusOf(ResultActions result) {
		return result == null ? -1 : result.andReturn().getResponse().getStatus();
	}

	private long usedBy(String customerId) {
		return jdbc.queryForObject("SELECT used FROM entitlement WHERE customer_id = ? AND feature = 'ai-credits'",
				Long.class, customerId);
	}

	private int storedEventCount(String eventKey) {
		return jdbc.queryForObject("SELECT count(*) FROM usage_event WHERE event_key = ?", Integer.class, eventKey);
	}

	private void grantQuota(String customerId, long quota) {
		jdbc.update("INSERT INTO entitlement (customer_id, feature, quota) VALUES (?, 'ai-credits', ?)",
				customerId, quota);
	}

	private ResultActions postEvent(String customerId, String eventKey, long units) throws Exception {
		return postEvent(customerId, eventKey, units, "2026-10-05T10:01:00Z");
	}

	private ResultActions postEvent(String customerId, String eventKey, long units, String occurredAt)
			throws Exception {
		String body = "{\"eventKey\": \"" + eventKey + "\", \"customerId\": \"" + customerId + "\","
				+ " \"userId\": \"dev42\", \"feature\": \"ai-credits\", \"units\": " + units
				+ ", \"occurredAt\": \"" + occurredAt + "\"}";
		return mockMvc.perform(post("/v1/usage-events").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
