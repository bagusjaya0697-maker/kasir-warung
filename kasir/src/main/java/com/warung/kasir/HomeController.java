
package com.warung.kasir;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "kasir";
    }

    @GetMapping("/produk")
    public String products() {
        return "produk";
    }
}
