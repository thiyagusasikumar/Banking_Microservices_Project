package com.banking.accountservice.repository;

import com.banking.accountservice.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {

    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDescIdDesc(Long accountId);

    @Modifying
    @Query("delete from AccountStatusHistory history where history.accountId = :accountId")
    void deleteByAccountId(@Param("accountId") Long accountId);
}
