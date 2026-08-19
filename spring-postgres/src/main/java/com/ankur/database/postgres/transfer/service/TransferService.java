package com.ankur.database.postgres.transfer.service;

import com.ankur.database.postgres.transfer.entity.Account;
import com.ankur.database.postgres.transfer.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accounts;

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Source and destination must differ");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Always acquire locks in the same order. This prevents two opposite
        // transfers from deadlocking while the transaction updates both rows.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Account first = accounts.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account: " + firstId));
        Account second = accounts.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account: " + secondId));

        Account source = fromId.equals(firstId) ? first : second;
        Account destination = toId.equals(firstId) ? first : second;
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
    }

    public static class InsufficientFundsException extends RuntimeException {
    }
}
