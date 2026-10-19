package com.sssingenieros.azul.service;

import com.sssingenieros.azul.model.Card;
import com.sssingenieros.azul.model.Product;
import com.sssingenieros.azul.repository.CardRepository;
import com.sssingenieros.azul.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaService {

    private final CardRepository cardRepository;
    private final ProductRepository productRepository;

    public CuentaService(CardRepository cardRepository, ProductRepository productRepository) {
        this.cardRepository = cardRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo() {
        return tarjetaPrincipal().getBalance();
    }

    @Transactional
    public BigDecimal actualizarSaldo(BigDecimal nuevoSaldo) {
        Card card = tarjetaPrincipal();
        card.setBalance(nuevoSaldo);
        card.setLastModified(LocalDateTime.now());
        return cardRepository.save(card).getBalance();
    }

    @Transactional
    public Card emitirTarjeta(long productId, String holderName) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        Card card = new Card();
        card.setCardNumber(generarNumeroTarjeta());
        card.setHolderName(holderName);
        card.setStatus("ISSUED");
        card.setBalance(BigDecimal.ZERO);
        card.setProduct(product);
        card.setCreationDate(LocalDateTime.now());
        return cardRepository.save(card);
    }

    private long generarNumeroTarjeta() {
        long numero;
        do {
            numero = 1_000_000_000_000L + (long) (Math.random() * 8_999_999_999_999L);
        } while (cardRepository.findByCardNumber(numero).isPresent());
        return numero;
    }

    private Card tarjetaPrincipal() {
        return cardRepository.findFirstByOrderByCardIdAsc()
                .orElseThrow(() -> new IllegalStateException("No hay tarjetas en la base santander"));
    }
}
