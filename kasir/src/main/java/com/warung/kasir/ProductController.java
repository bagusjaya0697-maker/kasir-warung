
package com.warung.kasir;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    public ProductController(ProductRepository productRepository,
                             SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public record ProductRequest(
            String name,
            BigDecimal price,
            Integer stock
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            BigDecimal price,
            Integer stock
    ) {
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getPrice(), p.getStock()
        );
    }

    private void validate(ProductRequest request) {
        if (request == null
                || request.name() == null
                || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Nama produk wajib diisi."
            );
        }

        if (request.name().trim().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Nama produk maksimal 150 karakter."
            );
        }

        if (request.price() == null
                || request.price().compareTo(BigDecimal.ZERO) < 0
                || request.price().scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Harga produk tidak valid."
            );
        }

        if (request.stock() == null || request.stock() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stok tidak boleh negatif."
            );
        }
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ProductResponse create(@RequestBody ProductRequest request) {
        validate(request);

        Product product = new Product(
                request.name().trim(),
                request.price(),
                request.stock()
        );

        return toResponse(productRepository.save(product));
    }

    @PutMapping("/{id}")
    @Transactional
    public ProductResponse update(@PathVariable Long id,
                                  @RequestBody ProductRequest request) {
        validate(request);

        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Produk tidak ditemukan."
                ));

        product.setName(request.name().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());

        return toResponse(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Produk tidak ditemukan."
                ));

        if (saleItemRepository.existsByProduct_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Produk sudah memiliki riwayat penjualan. Ubah stok menjadi 0 jika produk tidak dijual lagi."
            );
        }

        productRepository.delete(product);
    }
}
