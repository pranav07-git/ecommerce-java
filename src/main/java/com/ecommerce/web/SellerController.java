package com.ecommerce.web;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repo.OrderRepository;
import com.ecommerce.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/seller")
public class SellerController {
    private final UserService users;
    private final ProductService products;
    private final OrderService orders;
    private final OrderRepository orderRepo;
    private final ReportService reports;

    public SellerController(UserService users, ProductService products, OrderService orders,
                            OrderRepository orderRepo, ReportService reports) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.orderRepo = orderRepo;
        this.reports = reports;
    }

    @GetMapping
    public String dashboard(Model m) {
        User seller = users.current();
        List<Product> mine = products.forSeller(seller);
        m.addAttribute("user", seller);
        m.addAttribute("products", mine);
        m.addAttribute("lowStock", mine.stream().filter(Product::isLowStock).toList());
        m.addAttribute("orders", orderRepo.findForSeller(seller));
        m.addAttribute("statuses", OrderStatus.values());
        m.addAttribute("report", reports.sellerReport(seller));
        m.addAttribute("invNames", mine.stream().map(Product::getName).toList());
        m.addAttribute("invStock", mine.stream().map(Product::getStock).toList());
        m.addAttribute("invLow", mine.stream().map(Product::getLowStockThreshold).toList());
        return "seller/dashboard";
    }

    @GetMapping("/products/new")
    public String newProduct(Model m) {
        m.addAttribute("form", new ProductForm());
        return formView(m, "List a new product", "/seller/products", "/seller");
    }

    @PostMapping("/products")
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult br, Model m, RedirectAttributes ra) {
        if (br.hasErrors()) return formView(m, "List a new product", "/seller/products", "/seller");
        Product p = products.create(form, users.current());
        ra.addFlashAttribute("message", "Product \"" + p.getName() + "\" listed successfully.");
        return "redirect:/seller";
    }

    @GetMapping("/products/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        Product p = products.requireManageable(id, users.current());
        m.addAttribute("form", products.toForm(p));
        return formView(m, "Edit product", "/seller/products/" + id, "/seller");
    }

    @PostMapping("/products/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form, BindingResult br,
                         Model m, RedirectAttributes ra) {
        if (br.hasErrors()) return formView(m, "Edit product", "/seller/products/" + id, "/seller");
        Product p = products.update(id, form, users.current());
        ra.addFlashAttribute("message", "Product \"" + p.getName() + "\" updated successfully.");
        return "redirect:/seller";
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        products.remove(id, users.current());
        ra.addFlashAttribute("message", "Product deleted successfully.");
        return "redirect:/seller";
    }

    @PostMapping("/products/{id}/stock")
    public String stock(@PathVariable Long id, @RequestParam int stock, RedirectAttributes ra) {
        Product p = products.setStock(id, stock, users.current());
        ra.addFlashAttribute("message", "Inventory for \"" + p.getName() + "\" updated to " + p.getStock() + ".");
        return "redirect:/seller#inventory";
    }

    @PostMapping("/orders/{id}/status")
    public String status(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
        Order o = orders.updateStatus(id, status, users.current());
        ra.addFlashAttribute("message", "Order #" + o.getId() + " is now " + o.getStatus() + ".");
        return "redirect:/seller#orders";
    }

    private String formView(Model m, String title, String action, String back) {
        m.addAttribute("title", title);
        m.addAttribute("action", action);
        m.addAttribute("back", back);
        return "product-form";
    }
}
