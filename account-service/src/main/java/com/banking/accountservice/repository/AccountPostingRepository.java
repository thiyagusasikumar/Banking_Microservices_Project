package com.banking.accountservice.repository;

import com.banking.accountservice.entity.AccountPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPostingRepository extends JpaRepository<AccountPosting, String> {
}
