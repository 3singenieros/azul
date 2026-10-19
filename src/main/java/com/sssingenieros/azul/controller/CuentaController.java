package com.sssingenieros.azul.controller;

import java.util.Calendar;
import java.util.Map;
import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;
import com.sssingenieros.azul.model.Card;
import com.sssingenieros.azul.service.CuentaService;
import org.springframework.transaction.annotation.Transactional;
import com.sssingenieros.azul.model.Product;
import com.sssingenieros.azul.repository.ProductRepository;
import com.sssingenieros.azul.repository.CardRepository;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@RestController
//@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final ProductRepository productRepository;
    private final CardRepository cardRepository;

    public CuentaController(CuentaService cuentaService, ProductRepository productRepository, CardRepository cardRepository) {
        this.cuentaService = cuentaService;
        this.productRepository = productRepository;
        this.cardRepository = cardRepository;
    }

    @GetMapping("/saldo")
    public Map<String, Object> consultarSaldo() {

        return Map.of(
            "cuenta", "100001",
            "saldo", cuentaService.consultarSaldo()
        );
    }

//Metodo para reibir el parametro @nuevoSaldo por queryString
@GetMapping("/actualizar-saldo")
public Map<String, Object> actualizarSaldo(@RequestParam("nuevoSaldo") BigDecimal nuevoSaldo) {
    return Map.of(
        "cuenta", "100001",
        "saldo", cuentaService.actualizarSaldo(nuevoSaldo)
    );
}



/*Metodo para emitir tarjeta con transactional
201 Created • 400 Bad Request • 404 Not Fount (productId inexistente)
*/
@PostMapping("/cards")
@Transactional
public Map<String, Object> emitirTarjeta(@RequestParam("productId") long productId,
        @RequestParam("holderName") String holderName) {

//generar numero de tarjeta aleatorio de 10 digitos
Long cardNumber = (long) (Math.random() * 10000000000L);

   Product product= productRepository.findByProductId(productId).orElse(null);
   if (product == null) {
    return Map.of("error", "404 Not Fount (productId inexistente)");
   }

   LocalDateTime startDate = Calendar.getInstance().getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
   //setear la fecha de inicio de la tarjeta a 3 años más
   LocalDateTime endDate = startDate.plusYears(3);
   
   Card card = new Card();
   card.setProduct(product);
   card.setHolderName(holderName);
   card.setStatus("ISSUED");
   card.setBalance(new BigDecimal(0));
   card.setCurrency("USD");
   card.setCardNumber(Long.parseLong(productId+""+cardNumber));
   card.setStartDate(startDate);
   card.setEndDate(endDate);
   card.setExpiresAt(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
   cardRepository.save(card);

/*
Se debe retornar una respuesta en este formato
{ “cardId”: “uuid-o-id”, “cardNumber”:  “1234567891234567”, “status”:  “ISSUED”, “expiresAt”: ”2027-01-18”, “balance”:  0.00, “currency”:  “USD”  } 
*/

return Map.of(
    "cardId", card.getCardId(),
    "cardNumber", card.getCardNumber(),
    "status", card.getStatus(),
    "expiresAt", card.getExpiresAt(),
    "balance", card.getBalance(),
    "currency", card.getCurrency()
);

}


/*
metodo POST /cards/{cardId}/actívate para activar la tarjeta
Response:
{ “cardId”: “uuid-o-id”, “Status”: “ACTIVE” } 
 Validación para activasción, STATUS debe ser ISSUED
 Si ya esta ACTIVE, se recomienda comportamiento idempotente (status response 200 - OK). 
 • Si esta BLOKED, no pude activarse. Status codes de la respuesta esperados (Se recomienda) 
 • 200 OK 
 • 404 Not Found • 409 Conflict (status invalido a asignar a la tarjeta)   
*/

@PostMapping("/cards/{cardId}/activate")
@Transactional
public Map<String, Object> activarTarjeta(@PathVariable("cardId") String cardId) {
    Card card = cardRepository.findByCardId(Long.parseLong(cardId)).orElse(null);
    if (card == null) {
        return Map.of("error", "404 Not Found");
    }
    if (card.getStatus().equals("ACTIVE")) {
        return Map.of("error", "409 Conflict");

    }
    if (card.getStatus().equals("BLOCKED")) {
        return Map.of("error", "409 Conflict");
    }
    card.setStatus("ACTIVE");
    cardRepository.save(card);
    return Map.of("cardId", card.getCardId(), "status", "ACTIVE");

}

/*
POST /cards/{cardId}/block 
Response:
{ “cardId”: “uuid-o-id”, “Status”: “BLOCKED” } 
 Validación para bloquear, STATUS debe ser ACTIVE
 Si ya esta BLOCKED, se recomienda comportamiento idempotente (status response 200 - OK). 
 • Si esta ISSUED, no pude bloquearse. Status codes de la respuesta esperados (Se recomienda) 
 • 200 OK 
 • 404 Not Found • 409 Conflict (status invalido a asignar a la tarjeta)   
*/

@PostMapping("/cards/{cardId}/block")
@Transactional
public Map<String, Object> bloquearTarjeta(@PathVariable("cardId") String cardId) {
    Card card = cardRepository.findByCardId(Long.parseLong(cardId)).orElse(null);
    if (card == null) {
        return Map.of("error", "404 Not Found");
    }
    if (card.getStatus().equals("BLOCKED")) {
        return Map.of("error", "409 Conflict");
    }
    if (card.getStatus().equals("ISSUED")) {
        return Map.of("error", "409 Conflict");
    }
    card.setStatus("BLOCKED");
    cardRepository.save(card);
    return Map.of("cardId", card.getCardId(), "status", "BLOCKED");
}


/*
GET /cards/{cardId} 
Obtiene el detalle de una tarjeta. 
Response:
{ “cardId”: “uuid-o-id”, “cardNumber”: “1234567891234567”, “holderName”: “Ricardo Perez”, “status”: “ACTIVE”, “blockedAt”: “”, “blockedReason”: “”, “expiresAt”: ”2027-01-18”, “balance”:  0.00, “currency”: “USD”  } 
*/

@GetMapping("/cards/{cardId}")
@Transactional
public Map<String, Object> obtenerTarjeta(@PathVariable("cardId") String cardId) {
    Card card = cardRepository.findByCardId(Long.parseLong(cardId)).orElse(null);
    if (card == null) {
        return Map.of("error", "404 Not Found");
    }
    return Map.of("cardId", card.getCardId(), "cardNumber", card.getCardNumber(), "holderName", card.getHolderName(), "status", card.getStatus(), "blockedAt", card.getBlockedAt(), "blockedReason", card.getBlockedReason(), "expiresAt", card.getExpiresAt(), "balance", card.getBalance(), "currency", card.getCurrency());
}


}







