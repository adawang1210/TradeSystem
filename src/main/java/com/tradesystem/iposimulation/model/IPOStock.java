package com.tradesystem.iposimulation.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * IPO listing metadata.
 */
public class IPOStock {

    private final String stockId;
    private final String stockName;
    private final String stockSymbol;
    private final BigDecimal price;
    private final int totalQuantity;
    private final LocalDateTime deadline;
    private final String issuerName;
    private volatile boolean drawExecuted;

    public IPOStock(
            String stockId,
            String stockName,
            String stockSymbol,
            BigDecimal price,
            int totalQuantity,
            LocalDateTime deadline,
            String issuerName) {
        this.stockId = Objects.requireNonNull(stockId, "stockId");
        this.stockName = Objects.requireNonNull(stockName, "stockName");
        this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol");
        this.price = price == null ? BigDecimal.ZERO : price;
        this.totalQuantity = totalQuantity;
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        this.issuerName = Objects.requireNonNull(issuerName, "issuerName");
        this.drawExecuted = false;
    }

    public String getStockId() {
        return stockId;
    }

    public String getStockName() {
        return stockName;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public boolean isDrawExecuted() {
        return drawExecuted;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(deadline);
    }

    public boolean isOpen(LocalDateTime now) {
        return !drawExecuted && now.isBefore(deadline);
    }

    public void markDrawExecuted() {
        this.drawExecuted = true;
    }
}
