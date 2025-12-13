package com.tradesystem.iposimulation.controller;

import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.service.InvestorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies common model attributes to every Thymeleaf view.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final InvestorService investorService;

    public GlobalModelAttributes(InvestorService investorService) {
        this.investorService = investorService;
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(HttpSession session) {
        if (session == null) {
            return null;
        }
        String userId = (String) session.getAttribute("CURRENT_USER");
        if (userId == null) {
            return null;
        }
        return investorService.findInvestor(userId)
                .map(Investor::getDisplayName)
                .orElse(userId);
    }
}
