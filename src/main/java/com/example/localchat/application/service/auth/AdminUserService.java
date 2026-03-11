package com.example.localchat.application.service.auth;

import com.example.localchat.adapters.rest.dto.request.UserUpdateRequest;
import com.example.localchat.adapters.rest.dto.response.UserDetailResponse;
import com.example.localchat.application.repository.DepartmentRepository;
import com.example.localchat.application.repository.TicketRepository;
import com.example.localchat.application.repository.UserRepository;
import com.example.localchat.domain.entity.Department;
import com.example.localchat.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserDetailResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToDetailResponse);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetails(String workerId) {
        User user = userRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("User not found: " + workerId));
        return mapToDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateUser(String workerId, UserUpdateRequest request) {
        User user = userRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("User not found: " + workerId));

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        
        if (request.isActive() != null) {
            user.setIsActive(request.isActive());
        }

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + request.departmentId()));
            user.setDepartment(dept);
        }

        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        log.info("Admin updated user: {}", workerId);
        return mapToDetailResponse(savedUser);
    }

    @Transactional
    public void deactivateUser(String workerId) {
        User user = userRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("User not found: " + workerId));
        user.setIsActive(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Admin deactivated user: {}", workerId);
    }

    private UserDetailResponse mapToDetailResponse(User user) {
        // Count tickets created by or assigned to this user
        long ticketsCreated = ticketRepository.findByCreatedByUserId(user.getUserId(), Pageable.unpaged()).getTotalElements();
        long ticketsAssigned = ticketRepository.findByAssignedToUserId(user.getUserId(), Pageable.unpaged()).getTotalElements();

        return UserDetailResponse.builder()
                .userId(user.getUserId())
                .workerId(user.getWorkerId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getDepartmentName() : "N/A")
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .assignedTicketCount(ticketsCreated + ticketsAssigned)
                .build();
    }
}
