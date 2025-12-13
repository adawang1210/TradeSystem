package com.tradesystem.iposimulation.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An investor's IPO application record.
 */
public class IPORecord {

    private final String recordId;
    private final String investorId;
    private final String stockId;
    private final int quantity;
    private final BigDecimal pricePerLot;
    private final LocalDateTime applyTime;
    private volatile Status status;
    private volatile String failureReason;

    public IPORecord(
            String recordId,
            String investorId,
            String stockId,
            int quantity,
            BigDecimal pricePerLot,
            LocalDateTime applyTime,
            Status initialStatus) {
        this.recordId = Objects.requireNonNull(recordId, "recordId");
        this.investorId = Objects.requireNonNull(investorId, "investorId");
        this.stockId = Objects.requireNonNull(stockId, "stockId");
        this.quantity = quantity;
        this.pricePerLot = pricePerLot == null ? BigDecimal.ZERO : pricePerLot;
        this.applyTime = applyTime == null ? LocalDateTime.now() : applyTime;
        this.status = initialStatus == null ? Status.PENDING : initialStatus;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getInvestorId() {
        return investorId;
    }

    public String getStockId() {
        return stockId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPricePerLot() {
        return pricePerLot;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markWon() {
        this.status = Status.WON;
        this.failureReason = null;
    }

    public void markLost() {
        this.status = Status.LOST;
        this.failureReason = null;
    }

    public void markFailed(Status failureStatus, String reason) {
        this.status = failureStatus;
        this.failureReason = reason;
    }
}
