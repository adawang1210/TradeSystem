package com.tradesystem.iposimulation.service;

import com.tradesystem.iposimulation.model.Investor;
import com.tradesystem.iposimulation.model.IPORecord;
import com.tradesystem.iposimulation.repository.DataRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class InvestorService {

    private final DataRepository repository;

    public InvestorService(DataRepository repository) {
        this.repository = repository;
    }

    public Collection<Investor> getAllInvestors() {
        return repository.findAllInvestors();
    }

    public Optional<Investor> findInvestor(String investorId) {
        return repository.findInvestor(investorId);
    }

    public Investor createInvestor(String name, BigDecimal initialBalance) {
        var investor = new Investor(repository.nextInvestorId(), name, initialBalance);
        repository.saveInvestor(investor);
        return investor;
    }

    public Investor registerInvestor(String investorId, String displayName, BigDecimal initialBalance) {
        if (repository.findInvestor(investorId).isPresent()) {
            throw new IllegalArgumentException("Investor already exists");
        }
        Investor investor = new Investor(investorId, displayName, initialBalance);
        repository.saveInvestor(investor);
        return investor;
    }

    public Investor loginOrCreate(String investorId) {
        return repository.findInvestor(investorId)
                .orElseGet(() -> {
                    Investor investor = new Investor(investorId, investorId, new BigDecimal("100000"));
                    repository.saveInvestor(investor);
                    return investor;
                });
    }

    public void addBalance(String investorId, BigDecimal amount) {
        repository.findInvestor(investorId).ifPresent(investor -> investor.addBalance(amount));
    }

    public List<IPORecord> history(String investorId) {
        return repository.findRecordsByInvestor(investorId);
    }

    public void deposit(String investorId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        Investor investor = repository.findInvestor(investorId)
                .orElseThrow(() -> new IllegalArgumentException("Investor not found"));
        investor.addBalance(amount);
        repository.saveInvestor(investor);
    }
}
