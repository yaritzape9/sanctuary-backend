package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.TwilioConfig;
import com.sanctuary.sanctuary_backend.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PanicServiceTest {

    @Mock
    private ContactService contactService;

    @Mock
    private TwilioConfig twilioConfig;

    @InjectMocks
    private PanicService panicService;

    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        lenient().when(twilioConfig.getPhoneNumber()).thenReturn("+15555550100");
    }

    @Test
    void triggerAlert_noContacts_returnsSilently() {
        when(contactService.getContacts(USER_ID)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> panicService.triggerAlert(USER_ID, 37.7749, -122.4194));

        verify(contactService).getContacts(USER_ID);
    }

    @Test
    void triggerAlert_withContacts_fetchesContactsAndDoesNotThrow() {
        Contact contact = mock(Contact.class);
        when(contact.getPhone()).thenReturn("+15555550111");
        when(contactService.getContacts(USER_ID)).thenReturn(List.of(contact));

        // The real Twilio SMS call happens inside sendToAll but is caught internally
        // per-contact, so this should never throw even without a live Twilio config.
        assertDoesNotThrow(() -> panicService.triggerAlert(USER_ID, 37.7749, -122.4194));

        verify(contactService).getContacts(USER_ID);
    }

    @Test
    void sendAllClear_noContacts_throwsRuntimeException() {
        when(contactService.getContacts(USER_ID)).thenReturn(Collections.emptyList());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> panicService.sendAllClear(USER_ID));

        assertDoesNotThrow(() -> {
            if (!ex.getMessage().contains(USER_ID)) {
                throw new AssertionError("Exception message should reference the user ID");
            }
        });
        verify(contactService).getContacts(USER_ID);
    }

    @Test
    void sendAllClear_withContacts_fetchesContactsAndDoesNotThrow() {
        Contact contact = mock(Contact.class);
        when(contact.getPhone()).thenReturn("+15555550111");
        when(contactService.getContacts(USER_ID)).thenReturn(List.of(contact));

        assertDoesNotThrow(() -> panicService.sendAllClear(USER_ID));

        verify(contactService).getContacts(USER_ID);
    }
}