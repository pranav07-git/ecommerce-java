package com.ecommerce;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.repo.OrderRepository;
import com.ecommerce.repo.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EcommerceFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Nova X")));
        mvc.perform(get("/").param("q", "mug")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Nova X"))));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void rolesAreEnforced() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin").with(user("buyer@shop.com").roles("BUYER"))).andExpect(status().isForbidden());
        mvc.perform(get("/seller").with(user("buyer@shop.com").roles("BUYER"))).andExpect(status().isForbidden());
        mvc.perform(get("/buyer").with(user("seller@shop.com").roles("SELLER"))).andExpect(status().isForbidden());
        // the H2 console exposes the whole database: admins only
        mvc.perform(get("/h2-console/")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/h2-console/").with(user("buyer@shop.com").roles("BUYER"))).andExpect(status().isForbidden());
        mvc.perform(get("/h2-console/").with(user("seller@shop.com").roles("SELLER"))).andExpect(status().isForbidden());
    }

    @Test
    void dashboardsRender() throws Exception {
        mvc.perform(get("/admin").with(user("admin@shop.com").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/activity").with(user("admin@shop.com").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Order #")));
        mvc.perform(get("/admin/users/new").with(user("admin@shop.com").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/products/new").with(user("admin@shop.com").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/seller").with(user("seller@shop.com").roles("SELLER"))).andExpect(status().isOk());
        mvc.perform(get("/seller/products/new").with(user("seller@shop.com").roles("SELLER"))).andExpect(status().isOk());
        mvc.perform(get("/buyer").with(user("buyer@shop.com").roles("BUYER"))).andExpect(status().isOk());
    }

    @Test
    void buyerPurchaseAndSellerFulfilment() throws Exception {
        Product lamp = products.findByActiveTrueOrderByIdDesc().stream()
                .filter(p -> p.getName().startsWith("Desk Lamp")).findFirst().orElseThrow();
        int before = lamp.getStock();
        var buyer = user("buyer@shop.com").roles("BUYER");
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/products/" + lamp.getId()).with(buyer).session(session)).andExpect(status().isOk());
        mvc.perform(post("/cart/add").param("productId", lamp.getId().toString()).param("qty", "2")
                .with(buyer).with(csrf()).session(session)).andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/cart").with(buyer).session(session)).andExpect(status().isOk());
        mvc.perform(post("/checkout").param("shippingAddress", "1 Test Road").param("payment", "Cash on Delivery")
                .with(buyer).with(csrf()).session(session)).andExpect(status().is3xxRedirection());

        assertEquals(before - 2, products.findById(lamp.getId()).orElseThrow().getStock());
        Order order = orders.findAllByOrderByCreatedAtDesc().get(0);
        assertEquals(OrderStatus.PLACED, order.getStatus());
        mvc.perform(get("/buyer/orders/" + order.getId()).with(buyer)).andExpect(status().isOk());

        // seller2 owns the lamp and ships it
        var seller2 = user("seller2@shop.com").roles("SELLER");
        mvc.perform(post("/seller/orders/" + order.getId() + "/status").param("status", "SHIPPED")
                .with(seller2).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals(OrderStatus.SHIPPED, orders.findById(order.getId()).orElseThrow().getStatus());

        // a seller with no items in the order cannot touch it
        mvc.perform(post("/seller/orders/" + order.getId() + "/status").param("status", "DELIVERED")
                .with(user("seller@shop.com").roles("SELLER")).with(csrf())).andExpect(status().isForbidden());

        // shipped orders can no longer be cancelled by the buyer
        mvc.perform(post("/buyer/orders/" + order.getId() + "/cancel").with(buyer).with(csrf()))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void cancelRestoresStock() throws Exception {
        Product mug = products.findByActiveTrueOrderByIdDesc().stream()
                .filter(p -> p.getName().startsWith("Ceramic")).findFirst().orElseThrow();
        int before = mug.getStock();
        var buyer = user("buyer@shop.com").roles("BUYER");
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add").param("productId", mug.getId().toString()).param("qty", "3")
                .with(buyer).with(csrf()).session(session));
        mvc.perform(post("/checkout").param("shippingAddress", "2 Test Road").with(buyer).with(csrf()).session(session));
        assertEquals(before - 3, products.findById(mug.getId()).orElseThrow().getStock());
        Order o = orders.findAllByOrderByCreatedAtDesc().get(0);
        mvc.perform(post("/buyer/orders/" + o.getId() + "/cancel").with(buyer).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals(before, products.findById(mug.getId()).orElseThrow().getStock());
    }

    @Test
    void cannotOrderMoreThanStock() throws Exception {
        Product hoodie = products.findByActiveTrueOrderByIdDesc().stream()
                .filter(p -> p.getName().startsWith("Cotton")).findFirst().orElseThrow();
        mvc.perform(post("/cart/add").param("productId", hoodie.getId().toString()).param("qty", "99")
                .with(user("buyer@shop.com").roles("BUYER")).with(csrf()).session(new MockHttpSession()))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void adminUserAndProductManagement() throws Exception {
        var admin = user("admin@shop.com").roles("ADMIN");
        mvc.perform(post("/admin/users").param("name", "New Seller").param("email", "new@shop.com")
                .param("role", "SELLER").param("password", "secret1").param("enabled", "true")
                .with(admin).with(csrf())).andExpect(redirectedUrl("/admin#users"));
        // duplicate email is rejected and re-renders the form
        mvc.perform(post("/admin/users").param("name", "Dup").param("email", "new@shop.com")
                .param("role", "BUYER").param("password", "secret1").with(admin).with(csrf()))
                .andExpect(status().isOk()).andExpect(view().name("admin/user-form"));
        // invalid product is rejected
        mvc.perform(post("/seller/products").param("name", "").param("price", "-1").param("stock", "1")
                .with(user("seller@shop.com").roles("SELLER")).with(csrf()))
                .andExpect(status().isOk()).andExpect(view().name("product-form"));
        // seller cannot edit another seller's product
        Product lamp = products.findByActiveTrueOrderByIdDesc().stream()
                .filter(p -> p.getName().startsWith("Desk Lamp")).findFirst().orElseThrow();
        mvc.perform(get("/seller/products/" + lamp.getId() + "/edit").with(user("seller@shop.com").roles("SELLER")))
                .andExpect(status().isForbidden());
    }
}
