package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.model.User;
import com.sanctuary.sanctuary_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, String> register(String email, String password) {
        // Block duplicate emails before saving
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setEmail(email);
        // Never store plain text passwords — hash before saving
        user.setPassword(passwordEncoder.encode(password));

        User saved = userRepository.save(user);

        // Return userId + token so the frontend can start making authenticated requests immediately
        String token = jwtUtil.generateToken(saved.getId());
        return Map.of("userId", saved.getId(), "token", token);
    }

    public Map<String, String> login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Compare submitted password against the stored hash
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId());
        return Map.of("userId", user.getId(), "token", token);
    }
}