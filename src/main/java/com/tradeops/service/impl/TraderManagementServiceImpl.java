package com.tradeops.service.impl;

import com.tradeops.annotation.Auditable;
import com.tradeops.exceptions.DuplicateResourceException;
import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.model.entity.Trader;
import com.tradeops.model.request.TraderRequests.CreateTraderRequest;
import com.tradeops.model.request.TraderRequests.UpdateTraderRequest;
import com.tradeops.repo.TraderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraderManagementServiceImpl {

    private final TraderRepo traderRepo;

    @Transactional
    @Auditable(action = "TRADER_CREATED", entityType = "TRADER")
    public Trader createTrader(CreateTraderRequest request) {
        if (traderRepo.findAll().stream().anyMatch(t -> t.getDomain().equals(request.domain()))) {
            throw new DuplicateResourceException("Domain already in use");
        }

        Trader trader = new Trader();
        trader.setLegalName(request.legalName());
        trader.setDisplayName(request.displayName());
        trader.setDomain(request.domain());
        trader.setStatus(Trader.TraderStatus.PENDING);
        trader.setThemeConfigJson("{\"primaryColor\": \"#000000\", \"layout\": \"standard\"}");

        return traderRepo.save(trader);
    }

    @Transactional
    @Auditable(action = "TRADER_STATUS_CHANGED", entityType = "TRADER")
    public Trader changeStatus(Long traderId, Trader.TraderStatus newStatus) {
        Trader trader = traderRepo.findById(traderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found"));
        trader.setStatus(newStatus);
        return traderRepo.save(trader);
    }

    @Transactional
    @Auditable(action = "TRADER_UPDATED", entityType = "TRADER")
    public Trader updateTrader(Long traderId, UpdateTraderRequest request) {
        Trader trader = traderRepo.findById(traderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found"));

        if (request.legalName() != null)
            trader.setLegalName(request.legalName());
        if (request.displayName() != null)
            trader.setDisplayName(request.displayName());
        if (request.domain() != null)
            trader.setDomain(request.domain());

        return traderRepo.save(trader);
    }

    @Transactional(readOnly = true)
    public java.util.List<Trader> getAllTraders() {
        return traderRepo.findAll();
    }
}
