package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwilioSmsSender implements SmsSender {

    private final TwilioConfig twilioConfig;

    @Override
    public void send(String to, String body) {
        Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(twilioConfig.getPhoneNumber()),
            body
        ).create();
    }
}