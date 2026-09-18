package com.blad.payments.transaction;

import com.blad.payments.account.Account;
import com.blad.payments.account.AccountRepository;
import com.blad.payments.common.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction transfer(UUID sourceId, UUID targetId,
                                BigDecimal amount, String idempotencyKey) {

        // 1. Idempotencia: si ya procesamos esta clave, devolvemos lo mismo
        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {}", idempotencyKey);
            return existing.get();
        }

        // 2. Validaciones de entrada
        if (sourceId.equals(targetId)) {
            throw new InvalidTransferException("Source and target must differ");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Amount must be positive");
        }

        // 3. Bloqueo ordenado para evitar deadlocks
        Account source, target;
        if (sourceId.compareTo(targetId) < 0) {
            source = lockAccount(sourceId);
            target = lockAccount(targetId);
        } else {
            target = lockAccount(targetId);
            source = lockAccount(sourceId);
        }

        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new InvalidTransferException("Currency mismatch");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        // 4. Movimiento de saldos
        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));

        // 5. Registro
        var tx = Transaction.builder()
                .sourceAccount(source)
                .targetAccount(target)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        return transactionRepository.save(tx);
    }

    private Account lockAccount(UUID id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}