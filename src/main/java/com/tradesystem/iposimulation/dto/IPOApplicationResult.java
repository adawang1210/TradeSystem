package com.tradesystem.iposimulation.dto;

import com.tradesystem.iposimulation.model.IPORecord;

public class IPOApplicationResult {

    private final boolean success;
    private final String message;
    private final IPORecord record;

    public IPOApplicationResult(boolean success, String message, IPORecord record) {
        this.success = success;
        this.message = message;
        this.record = record;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public IPORecord getRecord() {
        return record;
    }
}
