package com.tradesystem.iposimulation.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IPOStockTest {

    @Test
    void isExpiredShouldReturnTrueWhenNowAfterDeadline() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(1);
        IPOStock stock = new IPOStock("STK-1", "Acme", "ACM",
                new BigDecimal("10.00"), 100, deadline, "Issuer");

        boolean expired = stock.isExpired(LocalDateTime.now());

        assertThat(expired).isTrue();
    }

    @Test
    void isExpiredShouldReturnFalseWhenNowBeforeOrEqualDeadline() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(1);
        IPOStock stock = new IPOStock("STK-2", "Beta", "BET",
                new BigDecimal("20.00"), 50, deadline, "Issuer");

        boolean expiredBefore = stock.isExpired(LocalDateTime.now());
        boolean expiredAtDeadline = stock.isExpired(deadline);

        assertThat(expiredBefore).isFalse();
        assertThat(expiredAtDeadline).isFalse();
    }
}
