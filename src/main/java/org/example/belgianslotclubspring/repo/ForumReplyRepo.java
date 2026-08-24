package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.ForumReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ForumReplyRepo extends JpaRepository<ForumReply, Long> {

    long countByQuestionId(Long questionId);

    @Query("SELECT r FROM ForumReply r JOIN FETCH r.question WHERE r.id = :id")
    Optional<ForumReply> findByIdWithQuestion(@Param("id") Long id);
}
