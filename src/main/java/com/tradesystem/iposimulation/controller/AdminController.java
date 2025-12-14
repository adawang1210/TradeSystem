package com.tradesystem.iposimulation.controller;

import com.tradesystem.iposimulation.dto.DrawResult;
import com.tradesystem.iposimulation.dto.PublishIPOForm;
import com.tradesystem.iposimulation.model.Administrator;
import com.tradesystem.iposimulation.service.IPOService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final IPOService ipoService;

    public AdminController(IPOService ipoService) {
        this.ipoService = ipoService;
    }

    @GetMapping
    public String dashboard(Model model, HttpSession session) {
        Administrator admin = requireAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        model.addAttribute("publishForm", new PublishIPOForm());
        model.addAttribute("ipos", ipoService.listAllIPOs());
        return "admin/dashboard";
    }

    @PostMapping("/publish")
    public String publish(@ModelAttribute("publishForm") @Valid PublishIPOForm form,
                          BindingResult bindingResult,
                          Model model,
                          HttpSession session) {
        Administrator admin = requireAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("ipos", ipoService.listAllIPOs());
            return "admin/dashboard";
        }
        admin.publishIPO(form, ipoService);
        model.addAttribute("flashMessage", "IPO published successfully");
        return "redirect:/admin";
    }

    @PostMapping("/draw")
    public String draw(@RequestParam("stockId") String stockId,
                       @RequestParam(value = "refund", defaultValue = "false") boolean refund,
                       Model model,
                       HttpSession session) {
        Administrator admin = requireAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        DrawResult result = admin.executeDraw(stockId, refund, ipoService);
        model.addAttribute("flashMessage", "Draw completed: " + result.getWinners() + " winners, " + result.getLosers() + " losers");
        return "redirect:/admin";
    }

    private Administrator requireAdmin(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (Administrator) session.getAttribute("CURRENT_ADMIN");
    }
}
