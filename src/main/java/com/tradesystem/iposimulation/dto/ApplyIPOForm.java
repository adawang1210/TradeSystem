package com.tradesystem.iposimulation.dto;

import jakarta.validation.constraints.NotBlank;

public class ApplyIPOForm {

    private String investorId;

    @NotBlank
    private String stockId;

    public String getInvestorId() {
        return investorId;
    }

    public void setInvestorId(String investorId) {
        this.investorId = investorId;
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
    }
}
