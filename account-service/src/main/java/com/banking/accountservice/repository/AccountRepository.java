package com.banking.accountservice.repository;


import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import com.banking.accountservice.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    Long generateAccountNumber();

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndCustomerId(String accountNumber, Long customerId);

    List<Account> findByCustomerId(Long customerId);

    Optional<Account> findByIdAndCustomerId(Long id, Long customerId);

    Page<Account> findByAccountStatusAndAccountType(AccountStatus status, AccountType type, Pageable pageable);

    Page<Account> findByAccountStatus(
            AccountStatus status,
            Pageable pageable);

    Page<Account> findByAccountType(
            AccountType type,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
