package com.example.localchat.application.repository;

import com.example.localchat.domain.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketTicketIdOrderByCreatedAtDesc(Long ticketId);
}
