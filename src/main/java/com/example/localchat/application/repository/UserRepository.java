package com.example.localchat.application.repository;

import com.example.localchat.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByWorkerId(String workerId);

    Optional<User> findFirstByRoleAndDepartment_DepartmentId(String role, Long departmentId);

    Optional<User> findFirstByRole(String role);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(u.workerId) FROM User u WHERE u.workerId LIKE :prefix%")
    String findMaxWorkerIdByPrefix(@org.springframework.data.repository.query.Param("prefix") String prefix);
}
