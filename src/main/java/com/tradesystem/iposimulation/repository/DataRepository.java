package com.tradesystem.iposimulation.repository;

import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.model.IPOStock;
import com.tradesystem.iposimulation.model.Status;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory storage for investors, IPO listings, and application records.
 */
@Component
public class DataRepository {

    private final ConcurrentHashMap<String, Investor> investors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IPOStock> stocks = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<IPORecord> records = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, AtomicInteger> stockReservations = new ConcurrentHashMap<>();

    private final AtomicInteger investorSeq = new AtomicInteger(1000);
    private final AtomicInteger stockSeq = new AtomicInteger(2000);
    private final AtomicInteger recordSeq = new AtomicInteger(3000);

    public DataRepository() {
        seedDemoData();
    }

    private void seedDemoData() {
        var investor = new Investor(nextInvestorId(), "Demo Investor", new BigDecimal("50000"));
        investors.put(investor.getInvestorId(), investor);

        LocalDateTime now = LocalDateTime.now();
        saveStock(new IPOStock(nextStockId(), "TSMC", "2330", new BigDecimal("1000"), 10, now.minusMinutes(1), "TSMC"));
        saveStock(new IPOStock(nextStockId(), "MediaTek", "2454", new BigDecimal("1200"), 5, now.plusDays(2), "MediaTek"));

        saveStock(new IPOStock(nextStockId(), "Evergreen", "2603", new BigDecimal("150"), 100, now.plusDays(5), "Evergreen Marine"));
        saveStock(new IPOStock(nextStockId(), "Yang Ming", "2609", new BigDecimal("120"), 80, now.plusDays(5), "Yang Ming Marine"));

        saveStock(new IPOStock(nextStockId(), "Fubon Financial", "2881", new BigDecimal("60"), 500, now.plusDays(7), "Fubon"));
        saveStock(new IPOStock(nextStockId(), "Mega Financial", "2886", new BigDecimal("40"), 600, now.plusDays(7), "Mega"));

        saveStock(new IPOStock(nextStockId(), "Old Corp", "0000", new BigDecimal("200"), 50, now.minusDays(1), "Legacy Holdings"));
        saveStock(new IPOStock(nextStockId(), "Urgent Corp", "9999", new BigDecimal("300"), 30, now.plusHours(1), "Urgent Ventures"));
        saveStock(new IPOStock(nextStockId(), "Penny Stock", "1111", new BigDecimal("10"), 1000, now.plusDays(4), "Penny Inc"));
        saveStock(new IPOStock(nextStockId(), "Luxury Corp", "8888", new BigDecimal("5000"), 1, now.plusDays(6), "Luxury Holdings"));
    }

    public String nextInvestorId() {
        return "INV-" + investorSeq.incrementAndGet();
    }

    public String nextStockId() {
        return "STK-" + stockSeq.incrementAndGet();
    }

    public String nextRecordId() {
        return "REC-" + recordSeq.incrementAndGet();
    }

    public Collection<Investor> findAllInvestors() {
        return investors.values();
    }

    public Optional<Investor> findInvestor(String investorId) {
        return Optional.ofNullable(investors.get(investorId));
    }

    public synchronized Investor saveInvestor(Investor investor) {
        investors.put(investor.getInvestorId(), investor);
        return investor;
    }

    public IPOStock saveStock(IPOStock stock) {
        stocks.put(stock.getStockId(), stock);
        stockReservations.putIfAbsent(stock.getStockId(), new AtomicInteger(0));
        return stock;
    }

    public Optional<IPOStock> findStock(String stockId) {
        return Optional.ofNullable(stocks.get(stockId));
    }

    public List<IPOStock> findOpenStocks(LocalDateTime now) {
        return stocks.values().stream().filter(stock -> stock.isOpen(now)).toList();
    }

    public List<IPOStock> findAllStocks() {
        return List.copyOf(stocks.values());
    }

    public IPORecord saveRecord(IPORecord record) {
        records.add(record);
        return record;
    }

    public List<IPORecord> findRecordsByInvestor(String investorId) {
        return records.stream()
                .filter(record -> record.getInvestorId().equals(investorId))
                .collect(Collectors.toList());
    }

    public List<IPORecord> findRecordsByStock(String stockId) {
        return records.stream()
                .filter(record -> record.getStockId().equals(stockId))
                .collect(Collectors.toList());
    }

    public boolean hasRecord(String investorId, String stockId) {
        return records.stream()
                .anyMatch(record -> record.getInvestorId().equals(investorId) && record.getStockId().equals(stockId));
    }

    public List<IPORecord> findPendingByStock(String stockId) {
        return records.stream()
                .filter(record -> record.getStockId().equals(stockId) && record.getStatus() == Status.PENDING)
                .collect(Collectors.toList());
    }

    public boolean reserveStockLots(String stockId, int quantity, int maxLots) {
        stockReservations.putIfAbsent(stockId, new AtomicInteger(0));
        AtomicInteger counter = stockReservations.get(stockId);
        while (true) {
            int current = counter.get();
            if (current + quantity > maxLots) {
                return false;
            }
            if (counter.compareAndSet(current, current + quantity)) {
                return true;
            }
        }
    }

    public void releaseStockLots(String stockId, int quantity) {
        AtomicInteger counter = stockReservations.get(stockId);
        if (counter == null) {
            return;
        }
        counter.updateAndGet(value -> Math.max(0, value - quantity));
    }

    public void reset() {
        investors.clear();
        stocks.clear();
        records.clear();
        stockReservations.clear();
        seedDemoData();
    }
}
