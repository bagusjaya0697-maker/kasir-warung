
package com.warung.kasir;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public SaleService.SaleResponse checkout(
            @RequestBody SaleService.SaleRequest request) {
        return saleService.checkout(request);
    }
}
