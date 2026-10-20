package com.banking.accountservice.repository;

import com.banking.accountservice.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {

    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDescIdDesc(Long accountId);
}
