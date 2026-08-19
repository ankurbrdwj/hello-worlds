package com.ankur.database.postgres.transfer;

import com.ankur.database.postgres.transfer.entity.Account;
import com.ankur.database.postgres.transfer.repository.AccountRepository;
import com.ankur.database.postgres.transfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransferServiceIntegrationTest {

    @Autowired AccountRepository accounts;
    @Autowired TransferService transfers;

    @BeforeEach
    void resetAccounts() {
        accounts.deleteAll();
        accounts.saveAll(List.of(
                new Account(1L, new BigDecimal("100.00")),
                new Account(2L, new BigDecimal("0.00"))));
    }

    @Test
    void concurrentTransfersCannotSpendTheSameBalanceTwice() throws Exception {
        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    transfers.transfer(1L, 2L, new BigDecimal("100.00"));
                    return true;
                } catch (TransferService.InsufficientFundsException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        long successful = results.stream().filter(this::get).count();
        assertThat(successful).isEqualTo(1);
        assertThat(accounts.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(accounts.findById(2L).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void insufficientFundsLeavesBothAccountsUnchanged() {
        assertThatThrownBy(() ->
                transfers.transfer(1L, 2L, new BigDecimal("100.01")))
                .isInstanceOf(TransferService.InsufficientFundsException.class);

        assertThat(accounts.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(accounts.findById(2L).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
    }

    private boolean get(java.util.concurrent.Future<Boolean> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
