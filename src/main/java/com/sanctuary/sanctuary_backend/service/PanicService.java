package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.dto.PanicResult;
import com.sanctuary.sanctuary_backend.model.Contact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PanicService {

    static final String NO_LOCATION_TEXT = "Their location could not be shared.";

    // No user ID or name: the ID means nothing to the contact and would sit in Twilio's
    // message logs. A display name can go here once profiles store one (roadmap 5.1).
    static final String ALERT_TEXT =
        "🆘 Someone who listed you as an emergency contact on Sanctuary pressed their panic button and may need help. ";
    static final String ALL_CLEAR_TEXT =
        "✅ The Sanctuary user who sent you a panic alert is now safe. You can disregard the earlier alert.";

    private final ContactService contactService;
    private final SmsSender smsSender;

    public PanicResult triggerAlert(String userId, Double lat, Double lng) {
        List<Contact> contacts = contactService.getContacts(userId);

        if (contacts.isEmpty()) {
            return PanicResult.NO_CONTACTS;
        }

        // Never send a "?q=null,null" link: say plainly that the location didn't come through
        String location = (lat == null || lng == null)
            ? NO_LOCATION_TEXT
            : "Location: https://maps.google.com/?q=" + lat + "," + lng;

        return sendToAll(contacts, ALERT_TEXT + location);
    }

    public PanicResult sendAllClear(String userId) {
        List<Contact> contacts = contactService.getContacts(userId);

        if (contacts.isEmpty()) {
            return PanicResult.NO_CONTACTS;
        }

        return sendToAll(contacts, ALL_CLEAR_TEXT);
    }

    private PanicResult sendToAll(List<Contact> contacts, String body) {
        int sent = 0;
        for (Contact contact : contacts) {
            try {
                smsSender.send(contact.getPhone(), body);
                sent++;
            } catch (Exception e) {
                // Never log the phone or e.getMessage(): Twilio's error text can include the number
                log.error("SMS to contact {} failed: {}", contact.getId(), e.getClass().getSimpleName());
            }
        }
        int failed = contacts.size() - sent;
        log.info("Panic SMS: {} sent, {} failed", sent, failed);
        return new PanicResult(sent, failed);
    }
}
