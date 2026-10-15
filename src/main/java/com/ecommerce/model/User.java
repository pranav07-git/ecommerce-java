package com.ecommerce.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public final long id;
    public String name;
    public String email;
    public String salt;
    public String passwordHash;
    public Role role;
    public boolean enabled = true;
    public String phone = "";
    /** Buyer address book. */
    public final List<String> addresses = new ArrayList<>();
    /** Buyer payment methods: display labels only, never card numbers. */
    public final List<String> paymentMethods = new ArrayList<>();

    public User(long id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }
}
