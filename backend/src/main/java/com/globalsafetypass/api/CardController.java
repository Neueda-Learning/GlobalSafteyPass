package com.globalsafetypass.api;

import com.globalsafetypass.api.ApiDtos.CardUpdate;
import com.globalsafetypass.model.Card;
import com.globalsafetypass.repository.CardRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardRepository cards;
    public CardController(CardRepository cards) { this.cards = cards; }

    @GetMapping
    public List<Card> list() { return cards.findByUserIdOrderById(1L); }

    @PatchMapping("/{id}")
    public Card update(@PathVariable Long id, @Valid @RequestBody CardUpdate input) {
        Card card = cards.findById(id).orElseThrow(() -> new IllegalArgumentException("Card not found."));
        if (input.overseasEnabled() != null) card.setOverseasEnabled(input.overseasEnabled());
        if (input.frozen() != null) card.setFrozen(input.frozen());
        if (input.balance() != null) card.setBalance(input.balance());
        if (input.singleLimit() != null) card.setSingleLimit(input.singleLimit());
        if (input.dailyLimit() != null) card.setDailyLimit(input.dailyLimit());
        return cards.save(card);
    }
}

