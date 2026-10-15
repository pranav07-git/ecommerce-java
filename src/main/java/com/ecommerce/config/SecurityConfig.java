package com.ecommerce.config;

import com.ecommerce.repo.UserRepository;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email.trim().toLowerCase())
                .map(u -> org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .disabled(!u.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
    }

    @Bean
    AuthenticationSuccessHandler roleRedirect() {
        return (request, response, auth) -> {
            String target = auth.getAuthorities().stream().map(a -> a.getAuthority()).findFirst()
                    .map(r -> switch (r) {
                        case "ROLE_ADMIN" -> "/admin";
                        case "ROLE_SELLER" -> "/seller";
                        default -> "/buyer";
                    }).orElse("/");
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthenticationSuccessHandler roleRedirect) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/products/**", "/login", "/register", "/css/**", "/error").permitAll()
                .requestMatchers(PathRequest.toH2Console()).hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasRole("SELLER")
                .requestMatchers("/buyer/**", "/cart/**", "/checkout", "/wishlist/**").hasRole("BUYER")
                .anyRequest().authenticated())
            // H2 console is a separate servlet with its own forms and frames: exempt it from CSRF
            // and allow same-origin framing. Access still requires an admin login.
            .csrf(c -> c.ignoringRequestMatchers(PathRequest.toH2Console()))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .formLogin(f -> f.loginPage("/login").successHandler(roleRedirect).permitAll())
            .logout(l -> l.logoutSuccessUrl("/"));
        return http.build();
    }
}
