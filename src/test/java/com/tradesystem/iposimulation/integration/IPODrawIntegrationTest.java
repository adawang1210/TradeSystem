package com.tradesystem.iposimulation.integration;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
import com.tradesystem.iposimulation.dto.PublishIPOForm;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.model.IPOStock;
import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.model.Status;
import com.tradesystem.iposimulation.repository.DataRepository;
import com.tradesystem.iposimulation.service.AdminService;
import com.tradesystem.iposimulation.service.IPOService;
import com.tradesystem.iposimulation.service.InvestorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class IPODrawIntegrationTest {

    @Autowired
    private IPOService ipoService;

    @Autowired
    private InvestorService investorService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private DataRepository dataRepository;

    @BeforeEach
    void resetRepository() {
        dataRepository.reset();
    }

    @Test
    void fullFlowAllocatesSingleWinnerAfterDraw() {
        PublishIPOForm publishForm = new PublishIPOForm();
        publishForm.setStockName("TEST-01");
        publishForm.setStockSymbol("TEST-01");
        publishForm.setPrice(new BigDecimal("1000"));
        publishForm.setTotalQuantity(1);
        publishForm.setDeadline(LocalDateTime.now().plusSeconds(1));
        publishForm.setIssuerName("Test Issuer");
        IPOStock stock = adminService.publishIPO(publishForm);

        Investor userA = investorService.loginOrCreate("userA");
        Investor userB = investorService.loginOrCreate("userB");
        assertEquals(BigDecimal.ZERO, userA.getBalance(), "New investors should start with zero balance");
        assertEquals(BigDecimal.ZERO, userB.getBalance(), "New investors should start with zero balance");

        investorService.deposit("userA", new BigDecimal("10000"));
        investorService.deposit("userB", new BigDecimal("10000"));
        assertEquals(new BigDecimal("10000"), investorService.findInvestor("userA").orElseThrow().getBalance());
        assertEquals(new BigDecimal("10000"), investorService.findInvestor("userB").orElseThrow().getBalance());

        ApplyIPOForm formA = new ApplyIPOForm();
        formA.setInvestorId("userA");
        formA.setStockId(stock.getStockId());
        IPOApplicationResult resultA = ipoService.apply(formA);
        assertTrue(resultA.isSuccess(), "userA application should succeed");

        ApplyIPOForm formB = new ApplyIPOForm();
        formB.setInvestorId("userB");
        formB.setStockId(stock.getStockId());
        IPOApplicationResult resultB = ipoService.apply(formB);
        assertTrue(resultB.isSuccess(), "userB application should succeed");

        List<IPORecord> pendingRecords = dataRepository.findRecordsByStock(stock.getStockId());
        assertEquals(2, pendingRecords.size(), "Both applications must be stored");
        assertTrue(pendingRecords.stream().allMatch(record -> record.getStatus() == Status.PENDING),
                "All applications must remain pending prior to draw");

        awaitDeadlinePass();
        adminService.executeDraw(stock.getStockId(), true);

        List<IPORecord> finalizedRecords = dataRepository.findRecordsByStock(stock.getStockId());
        long won = finalizedRecords.stream().filter(record -> record.getStatus() == Status.WON).count();
        long lost = finalizedRecords.stream().filter(record -> record.getStatus() == Status.LOST).count();
        assertEquals(1, won, "Exactly one winner should be selected");
        assertEquals(1, lost, "Exactly one loser should remain");

        IPORecord winningRecord = finalizedRecords.stream()
                .filter(record -> record.getStatus() == Status.WON)
                .findFirst()
                .orElseThrow();
        IPORecord losingRecord = finalizedRecords.stream()
                .filter(record -> record.getStatus() == Status.LOST)
                .findFirst()
                .orElseThrow();

        BigDecimal winnerBalance = investorService.findInvestor(winningRecord.getInvestorId()).orElseThrow().getBalance();
        BigDecimal loserBalance = investorService.findInvestor(losingRecord.getInvestorId()).orElseThrow().getBalance();

        assertEquals(new BigDecimal("9000"), winnerBalance, "Winner keeps funds deducted for allocation");
        assertEquals(new BigDecimal("10000"), loserBalance, "Loser should be refunded in full");
    }

    private void awaitDeadlinePass() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
