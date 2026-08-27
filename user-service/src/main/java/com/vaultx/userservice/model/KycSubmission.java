package com.vaultx.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_submissions")
@Getter
@Setter
public class KycSubmission {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 500)
    private String address;

    @Column(name = "document_ref", length = 255)
    private String documentRef;

    @Column(name = "selfie_ref", length = 255)
    private String selfieRef;

    @Column(length = 20, nullable = false)
    private String status = "VERIFIED";

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }
}
