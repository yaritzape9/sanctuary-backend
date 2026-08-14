package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.model.AuthProvider;
import com.sanctuary.sanctuary_backend.model.User;
import com.sanctuary.sanctuary_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Common stub: token generation always succeeds once we get that far
        lenient().when(jwtUtil.generateToken(any(), any())).thenReturn("fake-jwt-token");
    }

    // ---------- register ----------

    @Test
    void register_createsNewUser_whenEmailNotTaken() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        User saved = new User();
        saved.setId("user-1");
        saved.setEmail("new@test.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        Map<String, String> result = authService.register("new@test.com", "password123");

        assertEquals("user-1", result.get("userId"));
        assertEquals("fake-jwt-token", result.get("token"));
        verify(userRepository).save(argThat(u ->
            u.getEmail().equals("new@test.com") &&
            u.getPassword().equals("hashed-password") &&
            u.getProvider() == AuthProvider.LOCAL
        ));
    }

    @Test
    void register_throws_whenEmailAlreadyExists() {
        when(userRepository.findByEmail("taken@test.com"))
            .thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () ->
            authService.register("taken@test.com", "password123"));

        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_returnsToken_whenCredentialsValid() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user@test.com");
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        Map<String, String> result = authService.login("user@test.com", "password123");

        assertEquals("user-1", result.get("userId"));
        assertEquals("fake-jwt-token", result.get("token"));
    }

    @Test
    void login_throws_whenEmailNotFound() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            authService.login("nobody@test.com", "password123"));
    }

    @Test
    void login_throws_whenPasswordWrong() {
        User user = new User();
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
            authService.login("user@test.com", "wrong-password"));
    }

    @Test
    void login_throws_whenAccountIsGoogleOnly() {
        User user = new User();
        user.setPassword(null); // Google-only account

        when(userRepository.findByEmail("googleuser@test.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            authService.login("googleuser@test.com", "anypassword"));
        assertTrue(ex.getMessage().contains("Google"));
    }

    // ---------- oauthSync ----------

    @Test
    void oauthSync_returnsExistingUser_whenGoogleIdMatches() {
        User user = new User();
        user.setId("user-1");
        user.setGoogleId("google-123");

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(user));

        Map<String, String> result = authService.oauthSync("user@test.com", "User Name", "google-123");

        assertEquals("user-1", result.get("userId"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void oauthSync_autoLinksExistingLocalAccount_byEmail() {
        User existingLocalUser = new User();
        existingLocalUser.setId("user-1");
        existingLocalUser.setEmail("user@test.com");
        existingLocalUser.setProvider(AuthProvider.LOCAL);

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(existingLocalUser));
        when(userRepository.save(any(User.class))).thenReturn(existingLocalUser);

        Map<String, String> result = authService.oauthSync("user@test.com", "User Name", "google-123");

        assertEquals("user-1", result.get("userId"));
        verify(userRepository).save(argThat(u -> "google-123".equals(u.getGoogleId())));
    }

    @Test
    void oauthSync_createsNewUser_whenNoMatchFound() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newperson@test.com")).thenReturn(Optional.empty());

        User newUser = new User();
        newUser.setId("user-2");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        Map<String, String> result = authService.oauthSync("newperson@test.com", "New Person", "google-123");

        assertEquals("user-2", result.get("userId"));
        verify(userRepository).save(argThat(u ->
            u.getEmail().equals("newperson@test.com") &&
            u.getGoogleId().equals("google-123") &&
            u.getProvider() == AuthProvider.GOOGLE
        ));
    }
}