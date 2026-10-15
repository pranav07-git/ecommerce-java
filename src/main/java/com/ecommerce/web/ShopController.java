package com.ecommerce.web;

import com.ecommerce.model.Product;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.service.BuyerService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class ShopController {
    private final ProductService products;
    private final UserService users;
    private final BuyerService buyers;

    public ShopController(ProductService products, UserService users, BuyerService buyers) {
        this.products = products;
        this.users = users;
        this.buyers = buyers;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) BigDecimal min,
                       @RequestParam(required = false) BigDecimal max,
                       @RequestParam(required = false) String sort,
                       Model m) {
        m.addAttribute("products", products.search(q, category, min, max, sort));
        m.addAttribute("categories", products.categories());
        m.addAttribute("q", q);
        m.addAttribute("category", category);
        m.addAttribute("min", min);
        m.addAttribute("max", max);
        m.addAttribute("sort", sort);
        return "home";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model m) {
        Product p = products.getActive(id);
        User u = users.currentOrNull();
        boolean wished = false;
        if (u != null && u.getRole() == Role.BUYER) {
            buyers.recordView(u, p);
            wished = buyers.isWishlisted(u, p);
        }
        m.addAttribute("product", p);
        m.addAttribute("wished", wished);
        return "product";
    }
}
