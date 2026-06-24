package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository repo;
    private static final int MAX_CONTACTS = 5;

    public List<Contact> getContacts(String userId) {
        return repo.findByUserId(userId);
    }

    public Contact addContact(String userId, String name, String phone) {
        if (repo.countByUserId(userId) >= MAX_CONTACTS) {
            throw new IllegalStateException("Maximum of 5 emergency contacts allowed");
        }
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setName(name);
        contact.setPhone(phone);
        Contact saved = repo.save(contact);
        log.info("Contact added for user {}: {}", userId, saved.getId());
        return saved;
    }

    public Contact updateContact(String contactId, String userId, String name, String phone) {
        Contact contact = repo.findById(contactId)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        if (!contact.getUserId().equals(userId)) {
            throw new SecurityException("Contact does not belong to this user");
        }
        contact.setName(name);
        contact.setPhone(phone);
        Contact updated = repo.save(contact);
        log.info("Contact {} updated by user {}", contactId, userId);
        return updated;
    }

    public void deleteContact(String contactId, String userId) {
        Contact contact = repo.findById(contactId)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        if (!contact.getUserId().equals(userId)) {
            throw new SecurityException("Contact does not belong to this user");
        }
        repo.delete(contact);
        log.info("Contact {} deleted by user {}", contactId, userId);
    }
}
