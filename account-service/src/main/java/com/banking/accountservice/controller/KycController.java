package com.banking.accountservice.controller;

import com.banking.accountservice.constant.KycStatus;
import com.banking.accountservice.dto.kyc.ReviewKycRequest;
import com.banking.accountservice.dto.kyc.SubmitKycRequest;
import com.banking.accountservice.entity.KycApplication;
import com.banking.accountservice.repository.KycApplicationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {
    private final KycApplicationRepository repository;

    // Customer sends a document type and a reference to its privately stored document.
    // The customer ID is taken from the signed-in user's token, not from the request body.
    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public KycApplication submitKyc(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody SubmitKycRequest request) {
        Long signedInCustomerId = getCustomerId(jwt);

        if (repository.existsByCustomerIdAndStatus(signedInCustomerId, KycStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A KYC application is already pending");
        }
        if (repository.existsByCustomerIdAndStatus(signedInCustomerId, KycStatus.APPROVED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC is already approved");
        }

        // Save a new application with PENDING status for a staff member to review.
        KycApplication application = new KycApplication();
        application.setCustomerId(signedInCustomerId);
        application.setDocumentType(request.getDocumentType());
        application.setDocumentReference(request.getDocumentReference());
        application.setStatus(KycStatus.PENDING);
        application.setSubmittedAt(LocalDateTime.now());
        return repository.save(application);
    }

    // Customer can only see the latest KYC application linked to their own token.
    @GetMapping("/me")
    public KycApplication getMyKycStatus(@AuthenticationPrincipal Jwt jwt) {
        return repository.findFirstByCustomerIdOrderBySubmittedAtDesc(getCustomerId(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC application not found"));
    }

    // Only staff roles can reach this endpoint (see SecurityConfig).
    @GetMapping("/reviews/pending")
    public List<KycApplication> getPendingApplications() {
        return repository.findByStatusOrderBySubmittedAtAsc(KycStatus.PENDING);
    }

    // Staff sends APPROVED, REJECTED, or NEEDS_MORE_INFO as the decision.
    @PatchMapping("/reviews/{applicationId}")
    public KycApplication reviewKyc(@PathVariable Long applicationId,
                                    @AuthenticationPrincipal Jwt reviewer,
                                    @Valid @RequestBody ReviewKycRequest request) {
        KycStatus decision = request.getDecision();
        boolean allowedDecision = decision == KycStatus.APPROVED
                || decision == KycStatus.REJECTED
                || decision == KycStatus.NEEDS_MORE_INFO;

        if (!allowedDecision) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED, REJECTED, or NEEDS_MORE_INFO");
        }
        if (decision == KycStatus.REJECTED && (request.getNote() == null || request.getNote().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required when rejecting KYC");
        }

        KycApplication application = repository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC application not found"));
        if (application.getStatus() != KycStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending applications can be reviewed");
        }

        // Save the decision and basic audit details.
        application.setStatus(decision);
        application.setReviewNote(request.getNote());
        application.setReviewerSubject(reviewer.getSubject());
        application.setReviewedAt(LocalDateTime.now());
        return repository.save(application);
    }

    // Convert the customer_id claim from the token to the database ID used by Account Service.
    private Long getCustomerId(Jwt jwt) {
        try {
            Long customerId = Long.parseLong(jwt.getClaimAsString("customer_id"));
            if (customerId > 0) {
                return customerId;
            }
        } catch (NumberFormatException | NullPointerException ignored) {
            // Return one clear error for missing or invalid customer IDs.
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is missing a valid customer_id claim");
    }
}
