package com.pm.userservice.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false,name = "user_id",unique = true)
    private UUID userId;

    @Column(name = "balance",nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "reserved_balance",nullable = false)
    private BigDecimal reserveBalance = BigDecimal.ZERO;

    @Column(length = 5,nullable = false)
    private String currency = "RUPEE";

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
            updatedAt = LocalDateTime.now();
        }
        if(id == null)
            id = UUID.randomUUID();
    }

    @PreUpdate
    void onUpdate(){
        if(updatedAt == null)
            updatedAt = LocalDateTime.now();
    }
}
