package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Contact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PanicServiceTest {

    @Mock
    private ContactService contactService;

    @Mock
    private SmsSender smsSender;

    @InjectMocks
    private PanicService panicService;

    @Test
    void triggerAlert_noContacts_doesNotSendSms() {
        when(contactService.getContacts("user1")).thenReturn(List.of());

        panicService.triggerAlert("user1", 37.77, -122.41);

        verify(smsSender, never()).send(anyString(), anyString());
    }

    @Test
    void triggerAlert_withContacts_sendsSmsToEach() {
        Contact c1 = new Contact();
        c1.setPhone("+15551110001");
        Contact c2 = new Contact();
        c2.setPhone("+15551110002");
        when(contactService.getContacts("user1")).thenReturn(List.of(c1, c2));

        panicService.triggerAlert("user1", 37.77, -122.41);

        verify(smsSender).send(eq("+15551110001"), contains("panic button"));
        verify(smsSender).send(eq("+15551110002"), contains("panic button"));
    }

    @Test
    void triggerAlert_oneContactFails_stillSendsToOthers() {
        Contact c1 = new Contact();
        c1.setPhone("+15551110001");
        Contact c2 = new Contact();
        c2.setPhone("+15551110002");
        when(contactService.getContacts("user1")).thenReturn(List.of(c1, c2));
        doThrow(new RuntimeException("Twilio error")).when(smsSender).send(eq("+15551110001"), anyString());

        panicService.triggerAlert("user1", 37.77, -122.41);

        verify(smsSender).send(eq("+15551110002"), anyString());
    }

    @Test
    void sendAllClear_noContacts_throwsException() {
        when(contactService.getContacts("user1")).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> panicService.sendAllClear("user1"));
    }

    @Test
    void sendAllClear_withContacts_sendsSafeMessage() {
        Contact c1 = new Contact();
        c1.setPhone("+15551110001");
        when(contactService.getContacts("user1")).thenReturn(List.of(c1));

        panicService.sendAllClear("user1");

        verify(smsSender).send(eq("+15551110001"), contains("now safe"));
    }
}