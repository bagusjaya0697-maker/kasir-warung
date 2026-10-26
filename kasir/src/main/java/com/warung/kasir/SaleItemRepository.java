
package com.warung.kasir;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository
        extends JpaRepository<SaleItem, Long> {

    boolean existsByProduct_Id(Long productId);
}
