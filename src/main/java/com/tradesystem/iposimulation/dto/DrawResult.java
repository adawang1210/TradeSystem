package com.tradesystem.iposimulation.dto;

public class DrawResult {

    private final int allocatedLots;
    private final int totalPending;
    private final int winners;
    private final int losers;

    public DrawResult(int allocatedLots, int totalPending, int winners, int losers) {
        this.allocatedLots = allocatedLots;
        this.totalPending = totalPending;
        this.winners = winners;
        this.losers = losers;
    }

    public int getAllocatedLots() {
        return allocatedLots;
    }

    public int getTotalPending() {
        return totalPending;
    }

    public int getWinners() {
        return winners;
    }

    public int getLosers() {
        return losers;
    }
}
