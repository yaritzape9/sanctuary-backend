package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService service;

    @GetMapping("/{userId}")
    public List<Contact> getByUser(@PathVariable String userId) {
        return service.getByUser(userId);
    }

    @PostMapping
    public ResponseEntity<Contact> create(@RequestBody Contact contact) {
        return ResponseEntity.ok(service.create(contact));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contact> update(
        @PathVariable String id,
        @RequestBody Contact contact
    ) {
        return ResponseEntity.ok(service.update(id, contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}