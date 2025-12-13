package com.tradesystem.iposimulation.service;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
import com.tradesystem.iposimulation.model.IPOStock;
import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.model.Status;
import com.tradesystem.iposimulation.repository.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IPOServiceTest {

    private DataRepository repository;
    private InvestorService investorService;
    private IPOService ipoService;

    @BeforeEach
    void setUp() {
        repository = new DataRepository();
        repository.reset();
        investorService = new InvestorService(repository);
        ipoService = new IPOService(repository, investorService);
    }

    @Test
    void shouldApplySuccessfullyWhenAllChecksPass() {
        Investor investor = createInvestor("INV-TEST", new BigDecimal("1000.00"));
        IPOStock stock = createOpenStock("STK-TEST", new BigDecimal("10.00"), 100);
        ApplyIPOForm form = new ApplyIPOForm();
        form.setInvestorId(investor.getInvestorId());
        form.setStockId(stock.getStockId());

        IPOApplicationResult result = ipoService.apply(form);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Application submitted");
        assertThat(result.getRecord()).isNotNull();
        assertThat(repository.findRecordsByInvestor(investor.getInvestorId()))
                .hasSize(1)
                .first()
                .satisfies(record -> {
                    assertThat(record.getStatus()).isEqualTo(Status.PENDING);
                    assertThat(record.getQuantity()).isEqualTo(1);
                });
    }

    @Test
    void shouldFailWhenInvestorLacksFunds() {
        Investor investor = createInvestor("INV-LOW", new BigDecimal("5.00"));
        IPOStock stock = createOpenStock("STK-LOW", new BigDecimal("10.00"), 100);
        ApplyIPOForm form = new ApplyIPOForm();
        form.setInvestorId(investor.getInvestorId());
        form.setStockId(stock.getStockId());

        IPOApplicationResult result = ipoService.apply(form);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Insufficient balance");
        assertThat(result.getRecord()).isNotNull();
        assertThat(result.getRecord().getStatus()).isEqualTo(Status.FAILED_FUNDS);
        assertThat(result.getRecord().getFailureReason()).isEqualTo("Insufficient funds");

        assertThat(repository.findRecordsByInvestor(investor.getInvestorId()))
                .hasSize(1)
                .first()
                .extracting(record -> record.getStatus())
                .isEqualTo(Status.FAILED_FUNDS);
        assertThat(investor.getBalance()).isEqualByComparingTo("5.00");
    }

    private Investor createInvestor(String id, BigDecimal balance) {
        Investor investor = new Investor(id, "Investor " + id, balance);
        repository.saveInvestor(investor);
        return investor;
    }

    private IPOStock createOpenStock(String id, BigDecimal price, int totalQuantity) {
        IPOStock stock = new IPOStock(id, "Stock " + id, id.substring(0, 3),
                price, totalQuantity, LocalDateTime.now().plusDays(1), "Issuer");
        repository.saveStock(stock);
        return stock;
    }

}
