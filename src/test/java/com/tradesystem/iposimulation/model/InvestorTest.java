package com.tradesystem.iposimulation.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InvestorTest {

    @Test
    void addBalanceShouldIncreaseBalanceWhenAmountPositive() {
        Investor investor = new Investor("INV-1", "Alice", new BigDecimal("100.00"));

        investor.addBalance(new BigDecimal("25.50"));

        assertThat(investor.getBalance()).isEqualByComparingTo("125.50");
    }

    @Test
    void deductBalanceShouldSucceedWhenFundsSufficient() {
        Investor investor = new Investor("INV-1", "Alice", new BigDecimal("150.00"));

        boolean result = investor.deductBalance(new BigDecimal("50.00"));

        assertThat(result).isTrue();
        assertThat(investor.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void deductBalanceShouldFailWhenFundsInsufficient() {
        Investor investor = new Investor("INV-1", "Alice", new BigDecimal("30.00"));

        boolean result = investor.deductBalance(new BigDecimal("50.00"));

        assertThat(result).isFalse();
        assertThat(investor.getBalance()).isEqualByComparingTo("30.00");
    }
}
