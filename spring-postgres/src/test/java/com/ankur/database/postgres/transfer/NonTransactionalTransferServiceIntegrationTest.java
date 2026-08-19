package com.ankur.database.postgres.transfer;

import com.ankur.database.postgres.transfer.entity.Account;
import com.ankur.database.postgres.transfer.repository.AccountRepository;
import com.ankur.database.postgres.transfer.service.NonTransactionalTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NonTransactionalTransferServiceIntegrationTest {

    @Autowired AccountRepository accounts;
    @Autowired NonTransactionalTransferService transfers;

    @BeforeEach
    void resetAccounts() {
        accounts.deleteAll();
        accounts.saveAll(List.of(
                new Account(1L, new BigDecimal("100.00")),
                new Account(2L, new BigDecimal("0.00"))));
    }

    @Test
    void failureBetweenDebitAndCreditLeavesInconsistentState() {
        assertThatThrownBy(() -> transfers.transferAndFailAfterDebit(
                1L, 2L, new BigDecimal("40.00")))
                .isInstanceOf(IllegalStateException.class);

        // This is the bug: the source debit committed before the exception,
        // while the destination credit never happened.
        assertThat(accounts.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("60.00");
        assertThat(accounts.findById(2L).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
    }
}
