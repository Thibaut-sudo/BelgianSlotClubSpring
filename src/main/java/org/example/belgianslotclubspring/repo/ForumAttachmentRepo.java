package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.ForumAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumAttachmentRepo extends JpaRepository<ForumAttachment, Long> {

    long countByQuestionId(Long questionId);
}
