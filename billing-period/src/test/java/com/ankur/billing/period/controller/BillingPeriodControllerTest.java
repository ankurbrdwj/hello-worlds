package com.ankur.billing.period.controller;

import com.ankur.billing.period.entity.BillingPeriod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingPeriodControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnOkWhenRequestingPeriodForDate() {
        ResponseEntity<BillingPeriod> response = restTemplate.getForEntity(
                "/periods/by-date?date=2019-01-15",
                BillingPeriod.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        BillingPeriod period = response.getBody();
        assertNotNull(period);
        assertEquals(3, period.getPeriodNumber());
        assertEquals(LocalDate.of(2019, 1, 12), period.getStartDate());
        assertEquals(LocalDate.of(2019, 1, 18), period.getEndDate());
        assertEquals("2019-3", period.getPeriodId());
    }

    @Test
    void shouldReturnAllPeriodsForYear() {
        ResponseEntity<BillingPeriod[]> response = restTemplate.getForEntity(
                "/periods?year=2019",
                BillingPeriod[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        BillingPeriod[] periods = response.getBody();
        assertNotNull(periods);

        // First period
        assertEquals(1, periods[0].getPeriodNumber());
        assertEquals("2019-1", periods[0].getPeriodId());
        assertEquals(LocalDate.of(2019, 1, 1), periods[0].getStartDate());

        // Second period (first Saturday)
        assertEquals(2, periods[1].getPeriodNumber());
        assertEquals("2019-2", periods[1].getPeriodId());
        assertEquals(LocalDate.of(2019, 1, 5), periods[1].getStartDate());
    }
}