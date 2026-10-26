
package com.warung.kasir;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SaleService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public SaleService(ProductRepository productRepository,
                       SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public record SaleLineRequest(Long productId, Integer quantity) {
    }

    public record SaleRequest(
            List<SaleLineRequest> items,
            BigDecimal payment
    ) {
    }

    public record SaleResponse(
            Long saleId,
            BigDecimal total,
            BigDecimal payment,
            BigDecimal changeAmount,
            String message
    ) {
    }

    private static class PreparedItem {
        private final Product product;
        private final int quantity;
        private final BigDecimal subtotal;

        private PreparedItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
            this.subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Transactional
    public SaleResponse checkout(SaleRequest request) {
        if (request == null || request.items() == null
                || request.items().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Keranjang masih kosong."
            );
        }

        if (request.items().size() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Terlalu banyak jenis produk."
            );
        }

        if (request.payment() == null
                || request.payment().compareTo(BigDecimal.ZERO) <= 0
                || request.payment().scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Uang pembayaran tidak valid."
            );
        }

        Set<Long> productIds = new HashSet<>();
        for (SaleLineRequest line : request.items()) {
            if (line == null || line.productId() == null
                    || line.quantity() == null || line.quantity() <= 0
                    || line.quantity() > 100000
                    || !productIds.add(line.productId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Data produk atau jumlah barang tidak valid."
                );
            }
        }

        List<PreparedItem> preparedItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Kunci produk dalam urutan ID agar transaksi bersamaan
        // lebih kecil kemungkinannya mengalami deadlock.
        List<Long> sortedIds = productIds.stream().sorted().toList();

        for (Long productId : sortedIds) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Salah satu produk tidak ditemukan."
                    ));

            int quantity = request.items().stream()
                    .filter(line -> line.productId().equals(productId))
                    .findFirst()
                    .orElseThrow()
                    .quantity();

            if (product.getStock() < quantity) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stok " + product.getName()
                                + " tidak cukup. Stok tersedia: "
                                + product.getStock()
                );
            }

            PreparedItem item = new PreparedItem(product, quantity);
            preparedItems.add(item);
            total = total.add(item.subtotal);
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        if (request.payment().compareTo(total) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uang pembayaran kurang dari total belanja."
            );
        }

        BigDecimal payment = request.payment().setScale(
                2, RoundingMode.HALF_UP
        );
        BigDecimal change = payment.subtract(total);

        Sale sale = saleRepository.save(
                new Sale(total, payment, change)
        );

        for (PreparedItem item : preparedItems) {
            item.product.setStock(
                    item.product.getStock() - item.quantity
            );

            saleItemRepository.save(new SaleItem(
                    sale,
                    item.product,
                    item.product.getName(),
                    item.quantity,
                    item.product.getPrice(),
                    item.subtotal
            ));
        }

        // Seluruh perubahan disimpan sebagai satu transaksi database.
        // Jika proses gagal, perubahan stok dan transaksi dibatalkan.
        productRepository.flush();
        saleItemRepository.flush();

        return new SaleResponse(
                sale.getId(),
                total,
                payment,
                change,
                "Pembayaran berhasil."
        );
    }
}

