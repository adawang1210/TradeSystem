package com.tradesystem.iposimulation.service;

import com.tradesystem.iposimulation.dto.DrawResult;
import com.tradesystem.iposimulation.dto.PublishIPOForm;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.model.IPOStock;
import com.tradesystem.iposimulation.model.Status;
import com.tradesystem.iposimulation.repository.DataRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class AdminService {

    private final DataRepository repository;
    private final InvestorService investorService;

    public AdminService(DataRepository repository, InvestorService investorService) {
        this.repository = repository;
        this.investorService = investorService;
    }

    public IPOStock publishIPO(PublishIPOForm form) {
        IPOStock stock = new IPOStock(
                repository.nextStockId(),
                form.getStockName(),
                form.getStockSymbol(),
                form.getPrice(),
                form.getTotalQuantity(),
                form.getDeadline(),
                form.getIssuerName());
        repository.saveStock(stock);
        return stock;
    }

    public DrawResult executeDraw(String stockId, boolean refundLosers) {
        IPOStock stock = repository.findStock(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
        if (!stock.isExpired(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot draw before deadline");
        }
        if (stock.isDrawExecuted()) {
            throw new IllegalStateException("Draw already executed");
        }

        List<IPORecord> pending = repository.findPendingByStock(stockId);
        Collections.shuffle(pending);
        int remaining = stock.getTotalQuantity();
        int winners = 0;
        int losers = 0;

        for (IPORecord record : pending) {
            if (remaining >= record.getQuantity()) {
                record.markWon();
                winners++;
                remaining -= record.getQuantity();
            } else {
                record.markLost();
                losers++;
                if (refundLosers) {
                    investorService.findInvestor(record.getInvestorId())
                            .ifPresent(inv -> inv.addBalance(record.getPricePerLot().multiply(BigDecimal.valueOf(record.getQuantity()))));
                }
            }
        }

        stock.markDrawExecuted();
        return new DrawResult(stock.getTotalQuantity() - remaining, pending.size(), winners, losers);
    }
}
