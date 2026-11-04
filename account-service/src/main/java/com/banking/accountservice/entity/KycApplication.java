package com.banking.accountservice.entity;

import com.banking.accountservice.constant.KycStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_applications", indexes = @Index(name = "idx_kyc_customer_status", columnList = "customer_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class KycApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    // Private-storage object key/reference only; never store document bytes in this table.
    @Column(name = "document_reference", nullable = false, length = 500)
    private String documentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KycStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_subject", length = 100)
    private String reviewerSubject;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;
}
