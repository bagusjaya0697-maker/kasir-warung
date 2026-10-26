
package com.warung.kasir;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(
        value = "SELECT * FROM products WHERE id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
