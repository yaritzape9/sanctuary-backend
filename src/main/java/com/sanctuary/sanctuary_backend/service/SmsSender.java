package com.sanctuary.sanctuary_backend.service;

public interface SmsSender {
    void send(String to, String body);
}