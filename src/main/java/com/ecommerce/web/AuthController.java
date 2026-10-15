package com.ecommerce.web;

import com.ecommerce.model.Role;
import com.ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model m) {
        m.addAttribute("form", new UserForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid UserForm form, BindingResult br, Model m, RedirectAttributes ra) {
        if (form.getRole() == Role.ADMIN) br.rejectValue("role", "role", "Choose buyer or seller");
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            br.rejectValue("password", "password", "Password must be at least 6 characters");
        }
        if (br.hasErrors()) return "register";
        try {
            users.create(form.getName(), form.getEmail(), form.getPassword(), form.getRole(), form.getEmail());
        } catch (IllegalArgumentException e) {
            br.rejectValue("email", "email", e.getMessage());
            return "register";
        }
        ra.addFlashAttribute("message", "Account created. Please sign in.");
        return "redirect:/login";
    }
}
