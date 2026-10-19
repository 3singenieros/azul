package com.sssingenieros.azul.repository;

import com.sssingenieros.azul.model.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(Long productId);

    Optional<Product> findByCode(String code);
}
