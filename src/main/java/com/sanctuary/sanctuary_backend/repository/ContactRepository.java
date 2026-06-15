package com.sanctuary.sanctuary_backend.repository;

import com.sanctuary.sanctuary_backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, String> {
    List<Contact> findByUserId(String userId);
}