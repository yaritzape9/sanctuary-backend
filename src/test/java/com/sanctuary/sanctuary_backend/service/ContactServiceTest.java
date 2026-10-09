package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.model.Relationship;
import com.sanctuary.sanctuary_backend.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository repo;

    @InjectMocks
    private ContactService contactService;

    private Contact contactOwnedBy(String userId) {
        Contact contact = new Contact();
        contact.setId("contact-1");
        contact.setUserId(userId);
        contact.setName("Mom");
        contact.setPhone("+15551234567");
        contact.setRelationship(Relationship.PARENT);
        return contact;
    }

    @Test
    void updateContact_rejectsAnotherUsersContact() {
        when(repo.findById("contact-1")).thenReturn(Optional.of(contactOwnedBy("victim")));

        assertThatThrownBy(() -> contactService.updateContact(
                "contact-1", "attacker", "Changed", "+15550000000", Relationship.OTHER))
            .isInstanceOf(SecurityException.class);

        verify(repo, never()).save(any());
    }

    @Test
    void deleteContact_rejectsAnotherUsersContact() {
        when(repo.findById("contact-1")).thenReturn(Optional.of(contactOwnedBy("victim")));

        assertThatThrownBy(() -> contactService.deleteContact("contact-1", "attacker"))
            .isInstanceOf(SecurityException.class);

        verify(repo, never()).delete(any());
    }

    @Test
    void deleteContact_allowsOwner() {
        Contact contact = contactOwnedBy("owner");
        when(repo.findById("contact-1")).thenReturn(Optional.of(contact));

        contactService.deleteContact("contact-1", "owner");

        verify(repo).delete(contact);
    }

    @Test
    void updateContact_unknownContact_throwsNotFound() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.updateContact(
                "missing", "owner", "Name", "+15551234567", Relationship.OTHER))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addContact_enforcesFiveContactLimit() {
        when(repo.countByUserId("owner")).thenReturn(5L);

        assertThatThrownBy(() -> contactService.addContact(
                "owner", "Sixth", "+15551234567", Relationship.FRIEND))
            .isInstanceOf(IllegalStateException.class);

        verify(repo, never()).save(any());
    }
}
