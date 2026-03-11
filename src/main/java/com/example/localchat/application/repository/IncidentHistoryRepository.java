package com.example.localchat.application.repository;

import com.example.localchat.domain.entity.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {
    List<IncidentHistory> findByTicketTicketIdOrderByEventTimestampDesc(Long ticketId);
}
