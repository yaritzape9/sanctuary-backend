package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.TwilioConfig;
import com.sanctuary.sanctuary_backend.dto.PanicResult;
import com.sanctuary.sanctuary_backend.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PanicServiceTest {

    @Mock
    private ContactService contactService;

    @Mock
    private TwilioConfig twilioConfig;

    @Mock
    private SmsSender smsSender;

    @InjectMocks
    private PanicService panicService;

    private static final String USER_ID = "user-123";
    private static final String PHONE_1 = "+12125550111";
    private static final String PHONE_2 = "+12125550122";

    @BeforeEach
    void setUp() {
        lenient().when(twilioConfig.getPhoneNumber()).thenReturn("+15555550100");
    }

    // ── No contacts ────────────────────────────────────────────────────────

    @Test
    void triggerAlert_noContacts_returnsNoContacts_andSendsNothing() {
        when(contactService.getContacts(USER_ID)).thenReturn(Collections.emptyList());

        PanicResult result = panicService.triggerAlert(USER_ID, 37.7749, -122.4194);

        assertThat(result.status()).isEqualTo("no_contacts");
        verifyNoInteractions(smsSender);
    }

    @Test
    void sendAllClear_noContacts_returnsNoContacts_insteadOfThrowing() {
        when(contactService.getContacts(USER_ID)).thenReturn(Collections.emptyList());

        PanicResult result = panicService.sendAllClear(USER_ID);

        assertThat(result.status()).isEqualTo("no_contacts");
        verifyNoInteractions(smsSender);
    }

    // ── Send outcomes ──────────────────────────────────────────────────────

    @Test
    void triggerAlert_everyTextSent_returnsSent() {
        givenContacts(PHONE_1, PHONE_2);

        PanicResult result = panicService.triggerAlert(USER_ID, 37.7749, -122.4194);

        assertThat(result).isEqualTo(new PanicResult(2, 0));
        assertThat(result.status()).isEqualTo("sent");
    }

    @Test
    void triggerAlert_oneTextFails_returnsPartial_andStillTextsTheOthers() {
        givenContacts(PHONE_1, PHONE_2);
        doThrow(new RuntimeException("Twilio rejected " + PHONE_1)).when(smsSender).send(eq(PHONE_1), anyString());

        PanicResult result = panicService.triggerAlert(USER_ID, null, null);

        assertThat(result).isEqualTo(new PanicResult(1, 1));
        assertThat(result.status()).isEqualTo("partial");
        verify(smsSender).send(eq(PHONE_2), anyString());
    }

    @Test
    void triggerAlert_everyTextFails_returnsFailed() {
        givenContacts(PHONE_1, PHONE_2);
        doThrow(new RuntimeException("Twilio down")).when(smsSender).send(anyString(), anyString());

        PanicResult result = panicService.triggerAlert(USER_ID, null, null);

        assertThat(result).isEqualTo(new PanicResult(0, 2));
        assertThat(result.status()).isEqualTo("failed");
    }

    @Test
    void sendAllClear_everyTextFails_returnsFailed() {
        givenContacts(PHONE_1);
        doThrow(new RuntimeException("Twilio down")).when(smsSender).send(anyString(), anyString());

        assertThat(panicService.sendAllClear(USER_ID).status()).isEqualTo("failed");
    }

    // ── Message text ───────────────────────────────────────────────────────

    @Test
    void triggerAlert_withLocation_sendsMapLink() {
        givenContacts(PHONE_1);

        panicService.triggerAlert(USER_ID, 37.7749, -122.4194);

        assertThat(sentBody()).contains("Location: https://maps.google.com/?q=37.7749,-122.4194");
    }

    @Test
    void triggerAlert_withoutLocation_saysItCouldNotBeShared_andNeverSendsNullLink() {
        givenContacts(PHONE_1);

        panicService.triggerAlert(USER_ID, null, null);

        String body = sentBody();
        assertThat(body).contains("Their location could not be shared.");
        assertThat(body).doesNotContain("null").doesNotContain("maps.google.com");
    }

    @Test
    void triggerAlert_messageNeverContainsTheUserId() {
        givenContacts(PHONE_1);

        panicService.triggerAlert(USER_ID, 37.7749, -122.4194);

        assertThat(sentBody()).doesNotContain(USER_ID);
    }

    @Test
    void sendAllClear_messageNeverContainsTheUserId() {
        givenContacts(PHONE_1);

        panicService.sendAllClear(USER_ID);

        assertThat(sentBody()).doesNotContain(USER_ID).contains("is now safe");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void givenContacts(String... phones) {
        List<Contact> contacts = new java.util.ArrayList<>();
        for (int i = 0; i < phones.length; i++) {
            Contact contact = new Contact();
            contact.setId("contact-" + i);
            contact.setPhone(phones[i]);
            contacts.add(contact);
        }
        when(contactService.getContacts(USER_ID)).thenReturn(contacts);
    }

    private String sentBody() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(eq(PHONE_1), body.capture());
        return body.getValue();
    }
}
