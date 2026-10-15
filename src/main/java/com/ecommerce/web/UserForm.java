package com.ecommerce.web;

import com.ecommerce.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserForm {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotNull(message = "Role is required")
    private Role role = Role.BUYER;

    private boolean enabled = true;

    /** Required on create, optional on edit. */
    private String password;
}
