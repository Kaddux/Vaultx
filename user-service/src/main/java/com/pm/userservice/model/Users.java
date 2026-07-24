package com.pm.userservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false,name = "username")
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private String role;

    @Column(name = "kyc_status", length = 20, nullable = false)
    private String kycStatus = "PENDING";

    @Email
    @Column(unique = true,nullable = false, name = "email")
    private String email;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime created_at;

    @Column(nullable = false,name = "updated_at")
    private LocalDateTime updated_at;

    @Version
    private Long version;


    @PrePersist
    void OnCreate(){
        if(id == null)
            id = UUID.randomUUID();
        if(created_at == null)
            created_at = LocalDateTime.now();
    }
    @PreUpdate
    void OnUpdate(){
        updated_at = LocalDateTime.now();
    }
}
