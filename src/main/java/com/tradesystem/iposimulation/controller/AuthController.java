package com.tradesystem.iposimulation.controller;

import com.tradesystem.iposimulation.model.Administrator;
import com.tradesystem.iposimulation.service.InvestorService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@Validated
public class AuthController {

    private final InvestorService investorService;

    public AuthController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam("userId") @NotBlank String userId, HttpSession session) {
        if ("admin".equals(userId)) {
            Administrator admin = new Administrator("admin", "Administrator");
            session.setAttribute("CURRENT_ADMIN", admin);
            session.removeAttribute("CURRENT_USER");
            return "redirect:/admin";
        }
        session.removeAttribute("CURRENT_ADMIN");
        session.setAttribute("CURRENT_USER", userId);
        investorService.loginOrCreate(userId);
        return "redirect:/ipo/list";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam("userId") @NotBlank String userId,
                             @RequestParam("displayName") @NotBlank String displayName,
                             HttpSession session,
                             Model model) {
        if (investorService.findInvestor(userId).isPresent()) {
            model.addAttribute("error", "User ID already exists. Please choose another.");
            model.addAttribute("userId", userId);
            model.addAttribute("displayName", displayName);
            return "register";
        }
        investorService.registerInvestor(userId, displayName, BigDecimal.ZERO);
        session.setAttribute("CURRENT_USER", userId);
        return "redirect:/ipo/list";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
