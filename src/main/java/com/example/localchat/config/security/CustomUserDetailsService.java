package com.example.localchat.config.security;

import com.example.localchat.application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByWorkerId(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getWorkerId())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .accountLocked(user.getAccountLocked())
                        .disabled(!user.getIsActive())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with workerId: " + username));
    }
}
