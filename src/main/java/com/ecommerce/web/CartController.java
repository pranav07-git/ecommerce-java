package com.ecommerce.web;

import com.ecommerce.model.Order;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repo.AddressRepository;
import com.ecommerce.repo.PaymentMethodRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class CartController {
    public record CartLine(Product product, int quantity) {
        public BigDecimal subtotal() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    private final Cart cart;
    private final ProductService products;
    private final OrderService orders;
    private final UserService users;
    private final AddressRepository addresses;
    private final PaymentMethodRepository payments;

    public CartController(Cart cart, ProductService products, OrderService orders, UserService users,
                          AddressRepository addresses, PaymentMethodRepository payments) {
        this.cart = cart;
        this.products = products;
        this.orders = orders;
        this.users = users;
        this.addresses = addresses;
        this.payments = payments;
    }

    @GetMapping("/cart")
    public String view(Model m) {
        List<CartLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : List.copyOf(cart.items().entrySet())) {
            try {
                CartLine line = new CartLine(products.getActive(e.getKey()), e.getValue());
                lines.add(line);
                total = total.add(line.subtotal());
            } catch (IllegalArgumentException gone) {
                cart.remove(e.getKey()); // product was removed meanwhile
            }
        }
        User u = users.current();
        m.addAttribute("lines", lines);
        m.addAttribute("total", total);
        m.addAttribute("addresses", addresses.findByUser(u));
        m.addAttribute("payments", payments.findByUser(u));
        return "cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int qty, RedirectAttributes ra) {
        Product p = products.getActive(productId);
        if (qty < 1) throw new IllegalArgumentException("Quantity must be at least 1");
        int wanted = cart.items().getOrDefault(productId, 0) + qty;
        if (wanted > p.getStock()) {
            throw new IllegalStateException("Only " + p.getStock() + " of " + p.getName() + " in stock");
        }
        cart.add(productId, qty);
        ra.addFlashAttribute("message", p.getName() + " added to cart.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long productId, @RequestParam int qty) {
        Product p = products.getActive(productId);
        if (qty > p.getStock()) throw new IllegalStateException("Only " + p.getStock() + " in stock");
        cart.set(productId, qty);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long productId) {
        cart.remove(productId);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String shippingAddress, @RequestParam(required = false) String payment,
                           RedirectAttributes ra) {
        Order o = orders.place(users.current(), cart.items(), shippingAddress, payment);
        cart.clear();
        ra.addFlashAttribute("message", "Order #" + o.getId() + " confirmed! Total: $" + o.getTotal());
        return "redirect:/buyer/orders/" + o.getId();
    }
}
