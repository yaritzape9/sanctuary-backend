package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.GoogleTokenVerifier;
import com.sanctuary.sanctuary_backend.config.GoogleTokenVerifier.GoogleIdentity;
import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.exception.EmailAlreadyRegisteredException;
import com.sanctuary.sanctuary_backend.exception.InvalidCredentialsException;
import com.sanctuary.sanctuary_backend.exception.InvalidGoogleTokenException;
import com.sanctuary.sanctuary_backend.model.AuthProvider;
import com.sanctuary.sanctuary_backend.model.User;
import com.sanctuary.sanctuary_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifier googleTokenVerifier;

    public Map<String, String> register(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);

        // Block duplicate emails before saving
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User();
        user.setEmail(email);
        // Never store plain text passwords — hash before saving
        user.setPassword(passwordEncoder.encode(password));
        user.setProvider(AuthProvider.LOCAL);

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Two signups raced past the check above. The exception text holds SQL,
            // so it must not reach the generic handler that echoes messages.
            throw new EmailAlreadyRegisteredException();
        }

        // Return userId + token so the frontend can start making authenticated requests immediately
        String token = jwtUtil.generateToken(saved.getId(), saved.getRole());
        return Map.of("userId", saved.getId(), "token", token);
    }

    public Map<String, String> login(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword() == null) {
            throw new InvalidCredentialsException("This account uses Google sign-in. Please log in with Google.");
        }

        // Compare submitted password against the stored hash (matches() throws on null)
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return Map.of("userId", user.getId(), "token", token);
    }

    /**
     * Google sign-in. Identity comes only from the verified Google ID token.
     * An email that already has a password account is refused rather than auto-linked:
     * local emails aren't verified yet, so auto-linking would let someone who registered
     * a victim's email first take over the victim's Google sign-in (account pre-hijacking).
     */
    public Map<String, String> oauthSync(String idToken) {
        GoogleIdentity identity = googleTokenVerifier.verify(idToken)
            .orElseThrow(InvalidGoogleTokenException::new);

        // Fast path: returning Google user
        User user = userRepository.findByGoogleId(identity.googleId()).orElse(null);

        if (user == null) {
            String email = normalizeEmail(identity.email());
            if (userRepository.findByEmail(email).isPresent()) {
                throw new EmailAlreadyRegisteredException();
            }
            // Brand new user, Google-only, no password
            user = new User();
            user.setEmail(email);
            user.setGoogleId(identity.googleId());
            user.setProvider(AuthProvider.GOOGLE);
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return Map.of("userId", user.getId(), "token", token);
    }

    // One account per address: A@x.com and a@x.com are the same person.
    // Login's body is still an unvalidated map, so email can be null here.
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
