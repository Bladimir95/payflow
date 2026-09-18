package com.blad.payments.common.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Object id) {
        super("Account not found: " + id);
    }
}