package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.User;
import com.sanctuary.sanctuary_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public String mintTokenForEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NoSuchElementException("No user found with email: " + email));
        return jwtUtil.generateToken(user.getId());
    }
}