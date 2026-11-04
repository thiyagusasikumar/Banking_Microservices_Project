package com.banking.accountservice.repository;

import com.banking.accountservice.constant.KycStatus;
import com.banking.accountservice.entity.KycApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycApplicationRepository extends JpaRepository<KycApplication, Long> {
    Optional<KycApplication> findFirstByCustomerIdOrderBySubmittedAtDesc(Long customerId);
    List<KycApplication> findByStatusOrderBySubmittedAtAsc(KycStatus status);
    boolean existsByCustomerIdAndStatus(Long customerId, KycStatus status);
}
