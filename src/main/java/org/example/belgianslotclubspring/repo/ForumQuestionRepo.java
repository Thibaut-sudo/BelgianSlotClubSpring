package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.ForumQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ForumQuestionRepo extends JpaRepository<ForumQuestion, Long> {

    List<ForumQuestion> findByThemeIdOrderByCreatedAtDesc(Long themeId);

    Optional<ForumQuestion> findFirstByThemeIdOrderByCreatedAtDesc(Long themeId);

    long countByThemeId(Long themeId);

    @Query("""
            SELECT DISTINCT q FROM ForumQuestion q
            JOIN FETCH q.theme
            LEFT JOIN FETCH q.replies
            WHERE q.id = :id
            """)
    Optional<ForumQuestion> findDetailedById(@Param("id") Long id);
}
