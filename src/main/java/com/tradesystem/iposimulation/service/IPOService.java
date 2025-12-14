package com.tradesystem.iposimulation.service;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.DrawResult;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
import com.tradesystem.iposimulation.dto.PublishIPOForm;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.model.IPOStock;
import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.model.Status;
import com.tradesystem.iposimulation.repository.DataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IPOService {

    private static final Logger log = LoggerFactory.getLogger(IPOService.class);

    private final DataRepository repository;
    private final InvestorService investorService;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public IPOService(DataRepository repository, InvestorService investorService) {
        this.repository = repository;
        this.investorService = investorService;
    }

    public List<IPOStock> listOpenIPOs() {
        return repository.findOpenStocks(LocalDateTime.now());
    }

    public List<IPOStock> listAllIPOs() {
        return repository.findAllStocks();
    }

    public List<IPOStock> listIPOsForDisplay() {
        LocalDateTime now = LocalDateTime.now();
        return repository.findAllStocks().stream()
                .sorted(Comparator
                        .comparingInt((IPOStock stock) -> ipoStatusOrder(stock, now))
                        .thenComparing(IPOStock::getDeadline))
                .toList();
    }

    private int ipoStatusOrder(IPOStock stock, LocalDateTime now) {
        if (!stock.isDrawExecuted() && !stock.isExpired(now)) {
            return 0; // Available
        }
        if (!stock.isDrawExecuted() && stock.isExpired(now)) {
            return 1; // Ended (deadline passed, draw pending)
        }
        return 2; // Finished (draw executed)
    }

    public IPOApplicationResult apply(ApplyIPOForm form) {
        Optional<Investor> investorOpt = investorService.findInvestor(form.getInvestorId());
        if (investorOpt.isEmpty()) {
            return new IPOApplicationResult(false, "Investor not found", null);
        }
        Optional<IPOStock> stockOpt = repository.findStock(form.getStockId());
        if (stockOpt.isEmpty()) {
            return new IPOApplicationResult(false, "IPO not found", null);
        }

        Investor investor = investorOpt.get();
        IPOStock stock = stockOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (stock.getDeadline().isBefore(now)) {
            throw new RuntimeException("IPO application has ended.");
        }

        if (stock.isExpired(now)) {
            return new IPOApplicationResult(false, "IPO deadline passed", null);
        }
        if (repository.hasRecord(investor.getInvestorId(), stock.getStockId())) {
            return new IPOApplicationResult(false, "Duplicate application detected", null);
        }

        final int quantity = 1;
        BigDecimal requiredFunds = stock.getPrice();

        BigDecimal totalCost = requiredFunds.multiply(BigDecimal.valueOf(quantity));
        if (investor.getBalance().compareTo(totalCost) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        String lockKey = investor.getInvestorId() + ":" + stock.getStockId();
        Object mutex = locks.computeIfAbsent(lockKey, key -> new Object());

        synchronized (mutex) {
            // Recheck duplicate and balance after locking
            if (repository.hasRecord(investor.getInvestorId(), stock.getStockId())) {
                return new IPOApplicationResult(false, "Duplicate application detected", null);
            }
            boolean deducted = investor.deductBalance(requiredFunds);
            if (!deducted) {
                log.warn("FAILED_FUNDS investor={} stock={}", investor.getInvestorId(), stock.getStockId());
                throw new IllegalStateException("Insufficient balance");
            }

            IPORecord record = createRecord(investor, stock, quantity, Status.PENDING);
            repository.saveRecord(record);
            investor.appendRecord(record);
            log.info("Investor {} applied for {} ({})", investor.getInvestorId(), stock.getStockName(), record.getRecordId());
            return new IPOApplicationResult(true, "Application submitted", record);
        }
    }

    private IPORecord createRecord(Investor investor, IPOStock stock, int quantity, Status status) {
        return new IPORecord(
                repository.nextRecordId(),
                investor.getInvestorId(),
                stock.getStockId(),
                quantity,
                stock.getPrice(),
                LocalDateTime.now(),
                status
        );
    }

    public List<IPORecord> getHistory(String investorId) {
        return repository.findRecordsByInvestor(investorId);
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
