package com.sanctuary.sanctuary_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void webSocketConfigBeanIsRegistered() {
        // Confirms WebSocketConfig is being picked up as a @Configuration class
        // by the real application context — fails if it ever ends up back under
        // src/test (or any other package Spring's component scan doesn't cover).
        assertThat(applicationContext.getBean(WebSocketConfig.class)).isNotNull();
    }

    @Test
    void stompAuthInterceptorIsRegistered() {
        // WebSocketConfig wires this in via configureClientInboundChannel —
        // if WebSocketConfig isn't loaded, this bean existing alone wouldn't
        // prove the wiring happened, but its absence would still catch a
        // broken StompAuthInterceptor dependency.
        assertThat(applicationContext.getBean(StompAuthInterceptor.class)).isNotNull();
    }

    @Test
    void simpMessagingTemplateIsRegistered() {
        // SimpMessagingTemplate is only auto-created by Spring when
        // @EnableWebSocketMessageBroker is active on a loaded @Configuration
        // class. Its presence proves EnableWebSocketMessageBroker actually fired.
        assertThat(applicationContext.getBean(SimpMessagingTemplate.class)).isNotNull();
    }
}