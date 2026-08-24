package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.ForumTheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumThemeRepo extends JpaRepository<ForumTheme, Long> {

    List<ForumTheme> findByClubNameOrderBySortOrderAscTitleAsc(String clubName);

    Optional<ForumTheme> findByClubNameAndCode(String clubName, String code);
}
