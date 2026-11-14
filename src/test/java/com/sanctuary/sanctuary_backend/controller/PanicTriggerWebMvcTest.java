package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
import com.sanctuary.sanctuary_backend.dto.PanicResult;
import com.sanctuary.sanctuary_backend.model.Role;
import com.sanctuary.sanctuary_backend.service.PanicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A panic must never be rejected because of its location. Missing, partial, out-of-range or
 * non-numeric coordinates still send the alert, with no location. The response says truthfully
 * whether the texts went out: 200 sent/partial, 502 failed (safe to retry), 422 no contacts.
 */
@WebMvcTest(PanicController.class)
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=panic-trigger-test-secret-long-enough-for-hs256")
class PanicTriggerWebMvcTest {

    private static final String USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private PanicService panicService;

    @BeforeEach
    void stubService() {
        when(panicService.triggerAlert(anyString(), any(), any())).thenReturn(new PanicResult(1, 0));
        when(panicService.sendAllClear(anyString())).thenReturn(new PanicResult(1, 0));
    }

    private MockHttpServletRequestBuilder trigger() {
        return post("/api/panic/trigger")
            .header("Authorization", "Bearer " + jwtUtil.generateToken(USER_ID, Role.USER));
    }

    private MockHttpServletRequestBuilder trigger(String body) {
        return trigger().contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void validLocation_isPassedThrough() throws Exception {
        mockMvc.perform(trigger("{\"lat\":37.7749,\"lng\":-122.4194}"))
            .andExpect(status().isOk());

        verify(panicService).triggerAlert(USER_ID, 37.7749, -122.4194);
    }

    @Test
    void edgeOfRangeLocation_isPassedThrough() throws Exception {
        mockMvc.perform(trigger("{\"lat\":-90,\"lng\":180}"))
            .andExpect(status().isOk());

        verify(panicService).triggerAlert(USER_ID, -90.0, 180.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"lat\":null,\"lng\":null}",
        "{\"lat\":37.7749}",                    // only half a location
        "{\"lng\":-122.4194}",
        "{\"lat\":91,\"lng\":0}",               // out of range
        "{\"lat\":0,\"lng\":-180.5}",
        "{\"lat\":\"abc\",\"lng\":\"def\"}",    // not numbers
        "{\"lat\":\"37.7\",\"lng\":\"-122.4\"}",
        "{\"lat\":true,\"lng\":[1]}",
        "{\"lat\":1e400,\"lng\":0}"             // overflows to infinity
    })
    void badOrMissingLocation_stillSendsAlert_withoutLocation(String body) throws Exception {
        mockMvc.perform(trigger(body))
            .andExpect(status().isOk());

        verify(panicService).triggerAlert(eq(USER_ID), isNull(), isNull());
    }

    @Test
    void emptyBody_stillSendsAlert_withoutLocation() throws Exception {
        mockMvc.perform(trigger())
            .andExpect(status().isOk());

        verify(panicService).triggerAlert(eq(USER_ID), isNull(), isNull());
    }

    // sent, failed -> HTTP status and body status
    @ParameterizedTest
    @CsvSource({
        "2, 0, 200, sent",
        "1, 1, 200, partial",
        "0, 2, 502, failed",
        "0, 0, 422, no_contacts"
    })
    void trigger_reportsWhatActuallyHappened(int sent, int failed, int httpStatus, String bodyStatus) throws Exception {
        when(panicService.triggerAlert(anyString(), any(), any())).thenReturn(new PanicResult(sent, failed));

        mockMvc.perform(trigger("{}"))
            .andExpect(status().is(httpStatus))
            .andExpect(jsonPath("$.status").value(bodyStatus))
            .andExpect(jsonPath("$.sent").value(sent))
            .andExpect(jsonPath("$.failed").value(failed));
    }

    @ParameterizedTest
    @CsvSource({
        "2, 0, 200, sent",
        "1, 1, 200, partial",
        "0, 2, 502, failed",
        "0, 0, 422, no_contacts"
    })
    void safe_reportsWhatActuallyHappened(int sent, int failed, int httpStatus, String bodyStatus) throws Exception {
        when(panicService.sendAllClear(anyString())).thenReturn(new PanicResult(sent, failed));

        mockMvc.perform(post("/api/panic/safe")
                .header("Authorization", "Bearer " + jwtUtil.generateToken(USER_ID, Role.USER)))
            .andExpect(status().is(httpStatus))
            .andExpect(jsonPath("$.status").value(bodyStatus))
            .andExpect(jsonPath("$.sent").value(sent))
            .andExpect(jsonPath("$.failed").value(failed));
    }
}
