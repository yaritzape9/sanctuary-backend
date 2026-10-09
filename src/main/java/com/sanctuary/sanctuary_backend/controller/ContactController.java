package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.model.Relationship;
import com.sanctuary.sanctuary_backend.service.ContactService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ContactController {

    private final ContactService contactService;

    // userId always comes from the validated JWT, never from the path, body or query
    @GetMapping
    public ResponseEntity<List<Contact>> getContacts(Authentication authentication) {
        return ResponseEntity.ok(contactService.getContacts(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<?> addContact(
            @RequestBody AddContactRequest request,
            Authentication authentication) {
        try {
            Contact saved = contactService.addContact(
                authentication.getName(),
                request.getName(),
                request.getPhone(),
                request.getRelationship()
            );
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<?> updateContact(
            @PathVariable String contactId,
            @RequestBody UpdateContactRequest request,
            Authentication authentication) {
        try {
            Contact updated = contactService.updateContact(
                contactId,
                authentication.getName(),
                request.getName(),
                request.getPhone(),
                request.getRelationship()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<?> deleteContact(
            @PathVariable String contactId,
            Authentication authentication) {
        try {
            contactService.deleteContact(contactId, authentication.getName());
            return ResponseEntity.ok("Contact deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @Data
    static class AddContactRequest {
        private String name;
        private String phone;
        private Relationship relationship;
    }

    @Data
    static class UpdateContactRequest {
        private String name;
        private String phone;
        private Relationship relationship;
    }
}
