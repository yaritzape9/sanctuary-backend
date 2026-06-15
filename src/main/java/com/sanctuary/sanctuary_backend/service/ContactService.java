package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository repo;
    private static final int MAX_CONTACTS = 5;

    public List<Contact> getByUser(String userId) {
        return repo.findByUserId(userId);
    }

    public Contact create(Contact contact) {
        List<Contact> existing = repo.findByUserId(contact.getUserId());
        if (existing.size() >= MAX_CONTACTS) {
            throw new RuntimeException("Maximum of 5 emergency contacts allowed");
        }
        return repo.save(contact);
    }

    public Contact update(String id, Contact updated) {
        Contact existing = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Contact not found"));
        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setRelationship(updated.getRelationship());
        return repo.save(existing);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }
}