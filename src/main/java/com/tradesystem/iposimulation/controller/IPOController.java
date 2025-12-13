package com.tradesystem.iposimulation.controller;

import com.tradesystem.iposimulation.dto.ApplyIPOForm;
import com.tradesystem.iposimulation.dto.IPOApplicationResult;
import com.tradesystem.iposimulation.model.IPORecord;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ipo")
public class IPOController {

    private final IPOService ipoService;
    private final InvestorService investorService;

    public IPOController(IPOService ipoService, InvestorService investorService) {
        this.ipoService = ipoService;
        this.investorService = investorService;
    }

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        String redirect = requireUser(session);
        if (redirect != null) {
            return redirect;
        }
        String currentUser = (String) session.getAttribute("CURRENT_USER");
        List<IPORecord> existingRecords = ipoService.getHistory(currentUser);
        Set<String> appliedStockIds = existingRecords.stream()
                .map(IPORecord::getStockId)
                .collect(Collectors.toSet());
        model.addAttribute("ipos", ipoService.listOpenIPOs());
        model.addAttribute("applyForm", new ApplyIPOForm());
        model.addAttribute("appliedStockIds", appliedStockIds);
        return "ipo/list";
    }

    @PostMapping("/apply")
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

    @GetMapping("/records")
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

    private String requireUser(HttpSession session) {
        String userId = (String) session.getAttribute("CURRENT_USER");
        if (userId == null) {
            return "redirect:/login";
        }
        investorService.loginOrCreate(userId);
        return null;
    }
}
