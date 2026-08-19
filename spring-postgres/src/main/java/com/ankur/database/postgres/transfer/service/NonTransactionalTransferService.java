package com.ankur.database.postgres.transfer.service;

import com.ankur.database.postgres.transfer.entity.Account;
import com.ankur.database.postgres.transfer.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Deliberately broken teaching example. The method has no transaction boundary:
 * each repository call can commit independently, so a failure can leave a
 * debit without its matching credit and the row locks do not span the method.
 */
@Service
@RequiredArgsConstructor
public class NonTransactionalTransferService {

    private final AccountRepository accounts;

    public void transferAndFailAfterDebit(Long fromId, Long toId, BigDecimal amount) {
        Account source = accounts.findByIdForUpdate(fromId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account: " + fromId));
        Account destination = accounts.findByIdForUpdate(toId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account: " + toId));

        if (source.getBalance().compareTo(amount) < 0) {
            throw new TransferService.InsufficientFundsException();
        }

        source.setBalance(source.getBalance().subtract(amount));
        accounts.saveAndFlush(source); // commits independently without an outer transaction

        throw new IllegalStateException("Simulated failure before credit");

        // In a real broken implementation, the credit would happen here.
        // destination.setBalance(destination.getBalance().add(amount));
        // accounts.saveAndFlush(destination);
    }
}
