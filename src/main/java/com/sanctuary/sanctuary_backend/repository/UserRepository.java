package com.sanctuary.sanctuary_backend.repository;

import com.sanctuary.sanctuary_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    // Used during login to look up a user by email
    Optional<User> findByEmail(String email);
}