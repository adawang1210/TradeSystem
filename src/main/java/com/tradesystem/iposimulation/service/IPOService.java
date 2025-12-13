package com.tradesystem.iposimulation.service;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
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

        if (stock.isExpired(now)) {
            return new IPOApplicationResult(false, "IPO deadline passed", null);
        }
        if (repository.hasRecord(investor.getInvestorId(), stock.getStockId())) {
            return new IPOApplicationResult(false, "Duplicate application detected", null);
        }

        BigDecimal requiredFunds = stock.getPrice().multiply(BigDecimal.valueOf(form.getQuantity()));

        String lockKey = investor.getInvestorId() + ":" + stock.getStockId();
        Object mutex = locks.computeIfAbsent(lockKey, key -> new Object());

        synchronized (mutex) {
            // Recheck duplicate and balance after locking
            if (repository.hasRecord(investor.getInvestorId(), stock.getStockId())) {
                return new IPOApplicationResult(false, "Duplicate application detected", null);
            }
            boolean reserved = repository.reserveStockLots(stock.getStockId(), form.getQuantity(), stock.getTotalQuantity());
            if (!reserved) {
                IPORecord failed = createRecord(investor, stock, form.getQuantity(), Status.FAILED_SOLD_OUT);
                failed.markFailed(Status.FAILED_SOLD_OUT, "IPO sold out");
                repository.saveRecord(failed);
                investor.appendRecord(failed);
                log.warn("FAILED_SOLD_OUT investor={} stock={}", investor.getInvestorId(), stock.getStockId());
                return new IPOApplicationResult(false, "IPO sold out", failed);
            }

            boolean deducted = investor.deductBalance(requiredFunds);
            if (!deducted) {
                repository.releaseStockLots(stock.getStockId(), form.getQuantity());
                IPORecord failed = createRecord(investor, stock, form.getQuantity(), Status.FAILED_FUNDS);
                failed.markFailed(Status.FAILED_FUNDS, "Insufficient funds");
                repository.saveRecord(failed);
                investor.appendRecord(failed);
                log.warn("FAILED_FUNDS investor={} stock={}", investor.getInvestorId(), stock.getStockId());
                return new IPOApplicationResult(false, "Insufficient balance", failed);
            }

            IPORecord record = createRecord(investor, stock, form.getQuantity(), Status.PENDING);
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
}
