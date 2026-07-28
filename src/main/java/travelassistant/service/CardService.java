package travelassistant.service;

import travelassistant.model.Card;
import travelassistant.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    public Card getCard(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    @Transactional
    public Card enableOverseas(Long cardId) {
        Card card = getCard(cardId);
        card.setOverseasEnabled(true);
        return cardRepository.save(card);
    }

    @Transactional
    public Card increaseLimit(Long cardId, BigDecimal newLimit) {
        Card card = getCard(cardId);
        card.setDailyLimit(newLimit);
        return cardRepository.save(card);
    }

    @Transactional
    public Card freezeCard(Long cardId) {
        Card card = getCard(cardId);
        card.setFrozen(true);
        return cardRepository.save(card);
    }

    @Transactional
    public Card unfreezeCard(Long cardId) {
        Card card = getCard(cardId);
        card.setFrozen(false);
        return cardRepository.save(card);
    }
}
