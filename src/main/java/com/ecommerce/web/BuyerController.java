package com.ecommerce.web;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.User;
import com.ecommerce.repo.*;
import com.ecommerce.service.BuyerService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class BuyerController {
    private final UserService users;
    private final BuyerService buyers;
    private final OrderService orders;
    private final ProductService products;
    private final OrderRepository orderRepo;
    private final WishlistRepository wishlists;
    private final ViewHistoryRepository history;
    private final AddressRepository addresses;
    private final PaymentMethodRepository payments;

    public BuyerController(UserService users, BuyerService buyers, OrderService orders, ProductService products,
                           OrderRepository orderRepo, WishlistRepository wishlists, ViewHistoryRepository history,
                           AddressRepository addresses, PaymentMethodRepository payments) {
        this.users = users;
        this.buyers = buyers;
        this.orders = orders;
        this.products = products;
        this.orderRepo = orderRepo;
        this.wishlists = wishlists;
        this.history = history;
        this.addresses = addresses;
        this.payments = payments;
    }

    @GetMapping("/buyer")
    public String dashboard(Model m) {
        User u = users.current();
        m.addAttribute("user", u);
        m.addAttribute("history", history.findTop10ByUserOrderByViewedAtDesc(u));
        m.addAttribute("orders", orderRepo.findByBuyerOrderByCreatedAtDesc(u));
        m.addAttribute("wishlist", wishlists.findByUserOrderByAddedAtDesc(u));
        m.addAttribute("addresses", addresses.findByUser(u));
        m.addAttribute("payments", payments.findByUser(u));
        return "buyer/dashboard";
    }

    @GetMapping("/buyer/orders/{id}")
    public String order(@PathVariable Long id, Model m) {
        Order o = orders.getVisible(id, users.current());
        m.addAttribute("order", o);
        m.addAttribute("steps", List.of(OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED));
        return "buyer/order";
    }

    @PostMapping("/buyer/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        orders.cancelByBuyer(id, users.current());
        ra.addFlashAttribute("message", "Order #" + id + " cancelled.");
        return "redirect:/buyer/orders/" + id;
    }

    @PostMapping("/wishlist/toggle/{productId}")
    public String toggleWishlist(@PathVariable Long productId, @RequestParam(defaultValue = "product") String from,
                                 RedirectAttributes ra) {
        boolean added = buyers.toggleWishlist(users.current(), products.getActive(productId));
        ra.addFlashAttribute("message", added ? "Added to wishlist." : "Removed from wishlist.");
        return from.equals("dashboard") ? "redirect:/buyer#wishlist" : "redirect:/products/" + productId;
    }

    @PostMapping("/buyer/account")
    public String profile(@RequestParam String name, @RequestParam(required = false) String phone, RedirectAttributes ra) {
        buyers.updateProfile(users.current(), name, phone);
        ra.addFlashAttribute("message", "Profile updated.");
        return "redirect:/buyer#account";
    }

    @PostMapping("/buyer/addresses")
    public String addAddress(@RequestParam String label, @RequestParam String line, RedirectAttributes ra) {
        buyers.addAddress(users.current(), label, line);
        ra.addFlashAttribute("message", "Address saved.");
        return "redirect:/buyer#account";
    }

    @PostMapping("/buyer/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes ra) {
        buyers.deleteAddress(users.current(), id);
        ra.addFlashAttribute("message", "Address removed.");
        return "redirect:/buyer#account";
    }

    @PostMapping("/buyer/payments")
    public String addPayment(@RequestParam String label, RedirectAttributes ra) {
        buyers.addPayment(users.current(), label);
        ra.addFlashAttribute("message", "Payment method saved.");
        return "redirect:/buyer#account";
    }

    @PostMapping("/buyer/payments/{id}/delete")
    public String deletePayment(@PathVariable Long id, RedirectAttributes ra) {
        buyers.deletePayment(users.current(), id);
        ra.addFlashAttribute("message", "Payment method removed.");
        return "redirect:/buyer#account";
    }
}
