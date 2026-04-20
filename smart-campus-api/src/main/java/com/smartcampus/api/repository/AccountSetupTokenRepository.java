package com.smartcampus.api.repository;

import com.smartcampus.api.model.AccountSetupToken;
import com.smartcampus.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountSetupTokenRepository extends JpaRepository<AccountSetupToken, Long> {
    Optional<AccountSetupToken> findByUser(User user);
    void deleteByUser(User user);
    boolean existsByUser(User user);
}
