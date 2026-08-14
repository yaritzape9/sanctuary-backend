package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.model.AuthProvider;
import com.sanctuary.sanctuary_backend.model.Role;
import com.sanctuary.sanctuary_backend.model.User;
import com.sanctuary.sanctuary_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private DevAuthService devAuthService;

    private User buildUser(String email, Role role) {
        User user = new User();
        user.setId("user-123");
        user.setEmail(email);
        user.setProvider(AuthProvider.LOCAL);
        user.setRole(role);
        return user;
    }

    @Test
    void mintTokenForEmail_returnsTokenForExistingUser() {
        User user = buildUser("test@example.com", Role.USER);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user-123", Role.USER)).thenReturn("fake-jwt");

        String token = devAuthService.mintTokenForEmail("test@example.com");

        assertEquals("fake-jwt", token);
        verify(jwtUtil).generateToken("user-123", Role.USER);
    }

    @Test
    void mintTokenForEmail_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> devAuthService.mintTokenForEmail("missing@example.com"));
    }

    @Test
    void promoteToDev_updatesAndSavesUserRole() {
        User user = buildUser("test@example.com", Role.USER);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = devAuthService.promoteToDev("test@example.com");

        assertEquals(Role.DEV, result.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void promoteToDev_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> devAuthService.promoteToDev("missing@example.com"));
    }
}