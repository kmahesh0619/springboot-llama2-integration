package com.example.localchat.application.repository;

import com.example.localchat.domain.entity.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IncidentTypeRepository extends JpaRepository<IncidentType, Long> {
    Optional<IncidentType> findByTypeCode(String typeCode);
    Optional<IncidentType> findByTypeName(String typeName);
}
