package com.example.localchat.application.service.auth;

import com.example.localchat.application.repository.DepartmentRepository;
import com.example.localchat.application.repository.UserRepository;
import com.example.localchat.adapters.rest.dto.request.LoginRequest;
import com.example.localchat.adapters.rest.dto.request.RegisterRequest;
import com.example.localchat.adapters.rest.dto.response.AuthResponse;
import com.example.localchat.domain.entity.Department;
import com.example.localchat.domain.entity.User;
import com.example.localchat.application.usecase.AuthUseCase;
import com.example.localchat.config.security.CustomUserDetailsService;
import com.example.localchat.infrastructure.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Value("${security.lock.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.lock.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String workerId = request.workerId();
        
        if (workerId == null || workerId.isBlank() || "EMP".equals(workerId)) {
            workerId = generateSequentialWorkerId(request.role());
            log.info("Auto-generated workerId: {} for role: {}", workerId, request.role());
        }
        
        if (userRepository.findByWorkerId(workerId).isPresent()) {
            throw new RuntimeException("Worker ID already exists: " + workerId);
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.email());
        }

        Department dept = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new RuntimeException("Department not found: " + request.departmentId()));

        User user = User.builder()
                .workerId(workerId)
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .department(dept)
                .isActive(true)
                .failedAttempts(0)
                .accountLocked(false)
                .build();

        User savedUser = userRepository.save(user);

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getWorkerId())
                .password(savedUser.getPassword())
                .roles(savedUser.getRole())
                .accountLocked(savedUser.getAccountLocked())
                .disabled(!savedUser.getIsActive())
                .build();

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .workerId(savedUser.getWorkerId())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .message("User registered successfully")
                .build();
    }

    private String generateSequentialWorkerId(String role) {
        String prefix = switch (role.toUpperCase()) {
            case "WORKER" -> "EMP";
            case "SUPERVISOR" -> "SUP";
            case "MANAGER" -> "MGR";
            case "ADMIN" -> "ADM";
            default -> "USR";
        };

        String lastId = userRepository.findMaxWorkerIdByPrefix(prefix);
        int nextNumber = 1;

        if (lastId != null && lastId.length() > prefix.length()) {
            try {
                String numericPart = lastId.substring(prefix.length());
                nextNumber = Integer.parseInt(numericPart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Could not parse numeric part of last workerId: {}", lastId);
            }
        }

        return String.format("%s%03d", prefix, nextNumber);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.workerId());

        User user = userRepository.findByWorkerId(request.workerId())
                .orElseThrow(() -> new RuntimeException("Invalid worker ID or password"));

        if (user.getAccountLocked()) {
            if (!unlockWhenTimeExpired(user)) {
                throw new LockedException("Account is locked. Try again later.");
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.workerId(), request.password())
            );
        } catch (BadCredentialsException e) {
            increaseFailedAttempts(user);
            throw new BadCredentialsException("Invalid worker ID or password");
        }

        resetFailedAttempts(user);

        var userDetails = userDetailsService.loadUserByUsername(request.workerId());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .workerId(user.getWorkerId())
                .fullName(user.getFullName())
                .role(user.getRole())
                .message("Login successful")
                .build();
    }

    private void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailAttempts);

        if (newFailAttempts >= maxFailedAttempts) {
            lockAccount(user);
        }
        userRepository.save(user);
    }

    private void lockAccount(User user) {
        user.setAccountLocked(true);
        user.setLockTime(Instant.now());
        log.warn("Account locked: {}", user.getWorkerId());
    }

    private boolean unlockWhenTimeExpired(User user) {
        Instant lockTime = user.getLockTime();
        if (lockTime == null) return true;

        Instant unlockTime = lockTime.plus(lockDurationMinutes, ChronoUnit.MINUTES);
        if (unlockTime.isBefore(Instant.now())) {
            user.setAccountLocked(false);
            user.setLockTime(null);
            user.setFailedAttempts(0);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    private void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        userRepository.save(user);
    }
}
