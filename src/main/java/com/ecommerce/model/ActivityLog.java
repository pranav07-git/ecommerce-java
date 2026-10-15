package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actor;

    @Column(length = 500)
    private String message;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ActivityLog(String actor, String message) {
        this.actor = actor;
        this.message = message;
    }
}
