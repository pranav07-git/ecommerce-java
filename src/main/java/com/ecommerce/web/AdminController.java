package com.ecommerce.web;

import com.ecommerce.model.*;
import com.ecommerce.repo.OrderRepository;
import com.ecommerce.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService users;
    private final ProductService products;
    private final OrderService orders;
    private final OrderRepository orderRepo;
    private final ActivityService activity;

    public AdminController(UserService users, ProductService products, OrderService orders,
                           OrderRepository orderRepo, ActivityService activity) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.orderRepo = orderRepo;
        this.activity = activity;
    }

    @GetMapping
    public String dashboard(Model m) {
        m.addAttribute("users", users.all());
        m.addAttribute("products", products.allActive());
        m.addAttribute("orders", orderRepo.findAllByOrderByCreatedAtDesc());
        m.addAttribute("statuses", OrderStatus.values());
        m.addAttribute("me", users.current());
        return "admin/dashboard";
    }

    /** Polled by the dashboard every few seconds for real-time activity monitoring. */
    @GetMapping("/activity")
    @ResponseBody
    public List<Map<String, Object>> activityFeed() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss");
        return activity.latest().stream()
                .map(a -> Map.<String, Object>of("id", a.getId(), "time", a.getCreatedAt().format(fmt),
                        "actor", String.valueOf(a.getActor()), "message", String.valueOf(a.getMessage())))
                .toList();
    }

    // ---- users ----

    @GetMapping("/users/new")
    public String newUser(Model m) {
        m.addAttribute("form", new UserForm());
        return userForm(m, "Create user", "/admin/users", true);
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("form") UserForm form, BindingResult br, Model m, RedirectAttributes ra) {
        if (br.hasErrors()) return userForm(m, "Create user", "/admin/users", true);
        try {
            User u = users.create(form.getName(), form.getEmail(), form.getPassword(), form.getRole(), users.current().getEmail());
            ra.addFlashAttribute("message", "User " + u.getEmail() + " created successfully.");
        } catch (IllegalArgumentException e) {
            br.reject("user", e.getMessage());
            return userForm(m, "Create user", "/admin/users", true);
        }
        return "redirect:/admin#users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model m) {
        User u = users.get(id);
        UserForm f = new UserForm();
        f.setName(u.getName());
        f.setEmail(u.getEmail());
        f.setRole(u.getRole());
        f.setEnabled(u.isEnabled());
        m.addAttribute("form", f);
        return userForm(m, "Edit user", "/admin/users/" + id, false);
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @Valid @ModelAttribute("form") UserForm form, BindingResult br,
                             Model m, RedirectAttributes ra) {
        if (br.hasErrors()) return userForm(m, "Edit user", "/admin/users/" + id, false);
        try {
            User u = users.update(id, form.getName(), form.getEmail(), form.getRole(), form.isEnabled(),
                    form.getPassword(), users.current());
            ra.addFlashAttribute("message", "User " + u.getEmail() + " updated successfully.");
        } catch (IllegalArgumentException e) {
            br.reject("user", e.getMessage());
            return userForm(m, "Edit user", "/admin/users/" + id, false);
        }
        return "redirect:/admin#users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        ra.addFlashAttribute("message", users.delete(id, users.current()));
        return "redirect:/admin#users";
    }

    private String userForm(Model m, String title, String action, boolean creating) {
        m.addAttribute("title", title);
        m.addAttribute("action", action);
        m.addAttribute("creating", creating);
        m.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    // ---- products ----

    @GetMapping("/products/new")
    public String newProduct(Model m) {
        m.addAttribute("form", new ProductForm());
        return productForm(m, "Add product", "/admin/products");
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("form") ProductForm form, BindingResult br, Model m, RedirectAttributes ra) {
        if (br.hasErrors()) return productForm(m, "Add product", "/admin/products");
        Product p = products.create(form, users.current());
        ra.addFlashAttribute("message", "Product \"" + p.getName() + "\" listed successfully.");
        return "redirect:/admin#products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model m) {
        m.addAttribute("form", products.toForm(products.getActive(id)));
        return productForm(m, "Edit product", "/admin/products/" + id);
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form, BindingResult br,
                                Model m, RedirectAttributes ra) {
        if (br.hasErrors()) return productForm(m, "Edit product", "/admin/products/" + id);
        Product p = products.update(id, form, users.current());
        ra.addFlashAttribute("message", "Product \"" + p.getName() + "\" updated successfully.");
        return "redirect:/admin#products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        products.remove(id, users.current());
        ra.addFlashAttribute("message", "Product deleted successfully.");
        return "redirect:/admin#products";
    }

    private String productForm(Model m, String title, String action) {
        m.addAttribute("title", title);
        m.addAttribute("action", action);
        m.addAttribute("back", "/admin#products");
        m.addAttribute("sellers", users.sellers());
        return "product-form";
    }

    // ---- orders ----

    @PostMapping("/orders/{id}/status")
    public String status(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
        Order o = orders.updateStatus(id, status, users.current());
        ra.addFlashAttribute("message", "Order #" + o.getId() + " is now " + o.getStatus() + ".");
        return "redirect:/admin#orders";
    }
}
