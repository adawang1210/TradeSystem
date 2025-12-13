package com.tradesystem.iposimulation;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.model.IPOStock;
import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.model.Status;
import com.tradesystem.iposimulation.repository.DataRepository;
import com.tradesystem.iposimulation.service.IPOService;
import com.tradesystem.iposimulation.service.InvestorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IPOConcurrencyTest {

    private DataRepository repository;
    private InvestorService investorService;
    private IPOService ipoService;
    private IPOStock stock;

    @BeforeEach
    void setup() {
        repository = new DataRepository();
        investorService = new InvestorService(repository);
        ipoService = new IPOService(repository, investorService);
        stock = new IPOStock("STK-CONCUR", "Concurrent Corp", "CCP",
                new BigDecimal("100"), 10, LocalDateTime.now().plusDays(1), "Issuer");
        repository.saveStock(stock);

        for (int i = 0; i < 50; i++) {
            repository.saveInvestor(new Investor("INV-SIM-" + i, "Investor" + i, new BigDecimal("1000")));
        }
    }

    @Test
    void shouldNotOversellDuringConcurrentApplications() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch ready = new CountDownLatch(50);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<IPOApplicationResult>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                ApplyIPOForm form = new ApplyIPOForm();
                form.setInvestorId("INV-SIM-" + idx);
                form.setStockId(stock.getStockId());
                form.setQuantity(1);
                return ipoService.apply(form);
            }));
        }

        ready.await();
        start.countDown();

        List<IPOApplicationResult> results = new ArrayList<>();
        for (Future<IPOApplicationResult> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        long successCount = results.stream().filter(IPOApplicationResult::isSuccess).count();
        assertEquals(10, successCount, "Exactly 10 successful applications should be recorded");

        List<IPORecord> stockRecords = repository.findRecordsByStock(stock.getStockId());
        long pendingRecords = stockRecords.stream().filter(r -> r.getStatus() == Status.PENDING).count();
        assertEquals(10, pendingRecords, "Only 10 records should hold stock allocations");

        Set<String> successInvestors = stockRecords.stream()
                .filter(r -> r.getStatus() == Status.PENDING)
                .map(IPORecord::getInvestorId)
                .collect(Collectors.toSet());

        for (int i = 0; i < 50; i++) {
            String investorId = "INV-SIM-" + i;
            BigDecimal balance = repository.findInvestor(investorId).map(Investor::getBalance).orElse(BigDecimal.ZERO);
            if (successInvestors.contains(investorId)) {
                assertEquals(new BigDecimal("900"), balance, "Successful investor should have funds deducted");
            } else {
                assertEquals(new BigDecimal("1000"), balance, "Failed investors retain full balance");
            }
        }

        long soldOutFailures = stockRecords.stream().filter(r -> r.getStatus() == Status.FAILED_SOLD_OUT).count();
        assertTrue(soldOutFailures >= 40, "At least 40 investors should fail due to sold out stock");
    }
}
