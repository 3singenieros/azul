package com.sssingenieros.azul.repository;

import com.sssingenieros.azul.model.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardId(Long cardId);

    Optional<Card> findByCardNumber(Long cardNumber);

    Optional<Card> findFirstByOrderByCardIdAsc();

    Optional<Card> findByProductDbid(Long productDbid);
}
