package com.tradesystem.iposimulation.controller;

import com.tradesystem.iposimulation.repository.DataRepository;
import com.tradesystem.iposimulation.service.InvestorService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private TrackingInvestorService investorService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        investorService = new TrackingInvestorService(new DataRepository());
        controller = new AuthController(investorService);
    }

    @Test
    void adminLoginShouldStoreAdministratorInSession() {
        HttpSession session = mock(HttpSession.class);

        String view = controller.doLogin("admin", session);

        assertThat(view).isEqualTo("redirect:/admin");
        ArgumentCaptor<Object> adminCaptor = ArgumentCaptor.forClass(Object.class);
        verify(session).setAttribute(eq("CURRENT_ADMIN"), adminCaptor.capture());
        assertThat(adminCaptor.getValue()).isInstanceOfSatisfying(
                com.tradesystem.iposimulation.model.Administrator.class,
                admin -> assertThat(admin.getAdminId()).isEqualTo("admin"));
        verify(session).removeAttribute("CURRENT_USER");
        verifyNoMoreInteractions(session);
    }

    @Test
    void userLoginShouldStoreInvestorInSession() {
        HttpSession session = mock(HttpSession.class);

        String view = controller.doLogin("user1", session);

        assertThat(view).isEqualTo("redirect:/ipo/list");
        verify(session).removeAttribute("CURRENT_ADMIN");
        verify(session).setAttribute("CURRENT_USER", "user1");
        assertThat(investorService.lastLoginId).isEqualTo("user1");
    }

    private static class TrackingInvestorService extends InvestorService {

        private String lastLoginId;

        TrackingInvestorService(DataRepository repository) {
            super(repository);
        }

        @Override
        public com.tradesystem.iposimulation.model.Investor loginOrCreate(String investorId) {
            this.lastLoginId = investorId;
            return super.loginOrCreate(investorId);
        }
    }
}
