package com.tradesystem.iposimulation.model;

import com.tradesystem.iposimulation.dto.DrawResult;
import com.tradesystem.iposimulation.dto.PublishIPOForm;
import com.tradesystem.iposimulation.service.IPOService;

import java.util.Objects;

/**
 * Domain model representing a system administrator.
 */
public class Administrator {

    private final String adminId;
    private final String name;

    public Administrator(String adminId, String name) {
        this.adminId = Objects.requireNonNull(adminId, "adminId");
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getAdminId() {
        return adminId;
    }

    public String getName() {
        return name;
    }

    public void publishIPO(PublishIPOForm form, IPOService ipoService) {
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(ipoService, "ipoService").publishIPO(form);
    }

    public DrawResult executeDraw(String stockId, boolean refundLosers, IPOService ipoService) {
        Objects.requireNonNull(stockId, "stockId");
        return Objects.requireNonNull(ipoService, "ipoService").executeDraw(stockId, refundLosers);
    }
}
