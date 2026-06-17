package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.TwilioConfig;
import com.sanctuary.sanctuary_backend.model.Contact;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PanicService {

    private final ContactService contactService;
    private final TwilioConfig twilioConfig;

    public void triggerAlert(String userId, Double lat, Double lng) {
        List<Contact> contacts = contactService.getByUser(userId);

        if (contacts.isEmpty()) {
            throw new RuntimeException("No emergency contacts found for user: " + userId);
        }

        String locationUrl = "https://maps.google.com/?q=" + lat + "," + lng;
        String body = "🆘 " + userId + " has triggered their Sanctuary panic button and may need help. Location: " + locationUrl;

        sendToAll(contacts, body);
    }

    public void sendAllClear(String userId) {
        List<Contact> contacts = contactService.getByUser(userId);

        if (contacts.isEmpty()) {
            throw new RuntimeException("No emergency contacts found for user: " + userId);
        }

        String body = "✅ " + userId + " is now safe. You can disregard the earlier alert.";

        sendToAll(contacts, body);
    }

    private void sendToAll(List<Contact> contacts, String body) {
        for (Contact contact : contacts) {
            try {
                Message.creator(
                    new PhoneNumber(contact.getPhone()),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    body
                ).create();
                log.info("SMS sent to {}", contact.getPhone());
            } catch (Exception e) {
                log.error("Failed to send SMS to {}: {}", contact.getPhone(), e.getMessage());
            }
        }
    }
}