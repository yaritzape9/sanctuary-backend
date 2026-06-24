package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.service.ContactService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Contact>> getContacts(@PathVariable String userId) {
        return ResponseEntity.ok(contactService.getContacts(userId));
    }

    @PostMapping
    public ResponseEntity<?> addContact(@RequestBody AddContactRequest request) {
        try {
            Contact saved = contactService.addContact(
                request.getUserId(),
                request.getName(),
                request.getPhone()
            );
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<?> updateContact(
            @PathVariable String contactId,
            @RequestBody UpdateContactRequest request) {
        try {
            Contact updated = contactService.updateContact(
                contactId,
                request.getUserId(),
                request.getName(),
                request.getPhone()
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
            @RequestParam String userId) {
        try {
            contactService.deleteContact(contactId, userId);
            return ResponseEntity.ok("Contact deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @Data
    static class AddContactRequest {
        private String userId;
        private String name;
        private String phone;
    }

    @Data
    static class UpdateContactRequest {
        private String userId;
        private String name;
        private String phone;
    }
}
