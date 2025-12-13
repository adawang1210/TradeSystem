package com.tradesystem.iposimulation.model;

/**
 * Lifecycle status for an investor's IPO application.
 */
public enum Status {
    PENDING,
    WON,
    LOST,
    FAILED_FUNDS,
    FAILED_DUPLICATE,
    FAILED_DEADLINE,
    FAILED_SOLD_OUT
}
