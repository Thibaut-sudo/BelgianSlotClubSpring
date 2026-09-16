package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberAccountRepo extends JpaRepository<MemberAccount, Long> {

    Optional<MemberAccount> findByEmailIgnoreCase(String email);
}
