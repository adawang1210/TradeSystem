package com.tradesystem.iposimulation.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents an investor participating in IPO subscriptions.
 */
public class Investor {

    private final String investorId;
    private final String displayName;
    private BigDecimal balance;
    private final CopyOnWriteArrayList<IPORecord> applyHistory = new CopyOnWriteArrayList<>();

    public Investor(String investorId, String displayName, BigDecimal balance) {
        this.investorId = Objects.requireNonNull(investorId, "investorId");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.balance = balance == null ? BigDecimal.ZERO : balance;
    }

    public String getInvestorId() {
        return investorId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public synchronized void addBalance(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        balance = balance.add(amount);
    }

    /**
     * Deducts funds in a thread-safe manner.
     *
     * @return {@code true} if the deduction succeeded.
     */
    public synchronized boolean deductBalance(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return true;
        }
        if (balance.compareTo(amount) < 0) {
            return false;
        }
        balance = balance.subtract(amount);
        return true;
    }

    public void appendRecord(IPORecord record) {
        if (record != null) {
            applyHistory.add(record);
        }
    }

    public List<IPORecord> getApplyHistorySnapshot() {
        return List.copyOf(applyHistory);
    }
}
