package com.tradesystem.iposimulation.controller;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.service.IPOService;
import com.tradesystem.iposimulation.service.InvestorService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class IPOController {

    private final IPOService ipoService;
    private final InvestorService investorService;

    public IPOController(IPOService ipoService, InvestorService investorService) {
        this.ipoService = ipoService;
        this.investorService = investorService;
    }

    @GetMapping("/ipo/list")
    public String list(Model model, HttpSession session) {
        String redirect = requireUser(session);
        if (redirect != null) {
            return redirect;
        }
        String currentUser = (String) session.getAttribute("CURRENT_USER");
        Investor investor = investorService.findInvestor(currentUser)
                .orElseThrow(() -> new IllegalStateException("Investor not found"));
        List<IPORecord> existingRecords = ipoService.getHistory(currentUser);
        Set<String> appliedStockIds = existingRecords.stream()
                .map(IPORecord::getStockId)
                .collect(Collectors.toSet());
        Map<String, String> stockNames = ipoService.listAllIPOs().stream()
                .collect(Collectors.toMap(stock -> stock.getStockId(), stock -> stock.getStockName()));
        model.addAttribute("ipos", ipoService.listOpenIPOs());
        model.addAttribute("applyForm", new ApplyIPOForm());
        model.addAttribute("appliedStockIds", appliedStockIds);
        model.addAttribute("investor", investor);
        model.addAttribute("myRecords", existingRecords);
        model.addAttribute("stockNames", stockNames);
        return "ipo/list";
    }

    @PostMapping("/ipo/apply")
    public String apply(@ModelAttribute("applyForm") @Valid ApplyIPOForm form,
                        BindingResult bindingResult,
                        Model model,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("ipos", ipoService.listOpenIPOs());
            return "ipo/list";
        }
        String redirect = requireUser(session);
        if (redirect != null) {
            return redirect;
        }
        String currentUser = (String) session.getAttribute("CURRENT_USER");
        form.setInvestorId(currentUser);
        IPOApplicationResult result = ipoService.apply(form);
        if (!result.isSuccess()) {
            model.addAttribute("flashMessage", result.getMessage());
            model.addAttribute("ipos", ipoService.listOpenIPOs());
            model.addAttribute("applyForm", new ApplyIPOForm());
            return "ipo/list";
        }
        redirectAttributes.addFlashAttribute("flashMessage", result.getMessage());
        return "redirect:/ipo/records";
    }

    @GetMapping("/ipo/records")
    public String records(Model model, HttpSession session) {
        String redirect = requireUser(session);
        if (redirect != null) {
            return redirect;
        }
        String currentUser = (String) session.getAttribute("CURRENT_USER");
        List<IPORecord> history = ipoService.getHistory(currentUser);
        model.addAttribute("records", history);
        return "ipo/records";
    }

    @PostMapping("/investor/deposit")
    public String deposit(@RequestParam("amount") BigDecimal amount,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        String redirect = requireUser(session);
        if (redirect != null) {
            return redirect;
        }
        String currentUser = (String) session.getAttribute("CURRENT_USER");
        try {
            investorService.deposit(currentUser, amount);
            redirectAttributes.addFlashAttribute("flashMessage", "Deposit successful");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/ipo/list";
    }

    private String requireUser(HttpSession session) {
        String userId = (String) session.getAttribute("CURRENT_USER");
        if (userId == null) {
            return "redirect:/login";
        }
        investorService.loginOrCreate(userId);
        return null;
    }
}
