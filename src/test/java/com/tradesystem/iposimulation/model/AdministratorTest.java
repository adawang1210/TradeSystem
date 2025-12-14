package com.tradesystem.iposimulation.model;

import com.tradesystem.iposimulation.dto.DrawResult;
import com.tradesystem.iposimulation.dto.PublishIPOForm;
import com.tradesystem.iposimulation.repository.DataRepository;
import com.tradesystem.iposimulation.service.IPOService;
import com.tradesystem.iposimulation.service.InvestorService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdministratorTest {

    private TrackingIPOService ipoService;
    private Administrator administrator;

    AdministratorTest() {
        DataRepository repository = new DataRepository();
        InvestorService investorService = new InvestorService(repository);
        ipoService = new TrackingIPOService(repository, investorService);
        administrator = new Administrator("admin", "Administrator");
    }

    @Test
    void publishIPOShouldDelegateToService() {
        PublishIPOForm form = new PublishIPOForm();

        administrator.publishIPO(form, ipoService);

        assertThat(ipoService.lastPublishedForm).isSameAs(form);
    }

    @Test
    void executeDrawShouldDelegateToService() {
        DrawResult expected = new DrawResult(1, 1, 1, 0);
        ipoService.drawResult = expected;

        DrawResult result = administrator.executeDraw("STK-1", true, ipoService);

        assertThat(ipoService.lastDrawStockId).isEqualTo("STK-1");
        assertThat(ipoService.lastDrawRefund).isTrue();
        assertThat(result).isSameAs(expected);
    }

    private static class TrackingIPOService extends IPOService {
        private PublishIPOForm lastPublishedForm;
        private String lastDrawStockId;
        private Boolean lastDrawRefund;
        private DrawResult drawResult = new DrawResult(0, 0, 0, 0);

        TrackingIPOService(DataRepository repository, InvestorService investorService) {
            super(repository, investorService);
        }

        @Override
        public IPOStock publishIPO(PublishIPOForm form) {
            this.lastPublishedForm = form;
            return null;
        }

        @Override
        public DrawResult executeDraw(String stockId, boolean refundLosers) {
            this.lastDrawStockId = stockId;
            this.lastDrawRefund = refundLosers;
            return drawResult;
        }
    }
}
