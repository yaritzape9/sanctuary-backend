package com.sanctuary.sanctuary_backend.controller;

import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.model.Relationship;
import com.sanctuary.sanctuary_backend.model.Role;
import com.sanctuary.sanctuary_backend.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST and PUT /api/contacts through the real MVC stack: US-only phone numbers stored as
 * +1XXXXXXXXXX, length limits, and 400s that never echo what was submitted.
 */
@WebMvcTest(ContactController.class)
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=contact-webmvc-test-secret-long-enough-for-hs256")
class ContactWebMvcTest {

    private static final String USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private ContactService contactService;

    private MockHttpServletRequestBuilder add(String body) {
        return send(HttpMethod.POST, "/api/contacts", body);
    }

    private MockHttpServletRequestBuilder update(String body) {
        return send(HttpMethod.PUT, "/api/contacts/contact-1", body);
    }

    private MockHttpServletRequestBuilder send(HttpMethod method, String path, String body) {
        return request(method, path)
            .header("Authorization", "Bearer " + jwtUtil.generateToken(USER_ID, Role.USER))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(body);
    }

    private static String body(String name, String phone, String relationship) {
        return "{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\",\"relationship\":\"" + relationship + "\"}";
    }

    @Test
    void addContact_typedUsFormat_isStoredAsE164() throws Exception {
        when(contactService.addContact(anyString(), any(), any(), any())).thenReturn(new Contact());

        mockMvc.perform(add(body("Mom", "(212) 555-0100", "PARENT")))
            .andExpect(status().isOk());

        verify(contactService).addContact(USER_ID, "Mom", "+12125550100", Relationship.PARENT);
    }

    @Test
    void updateContact_typedUsFormat_isStoredAsE164() throws Exception {
        when(contactService.updateContact(anyString(), anyString(), any(), any(), any())).thenReturn(new Contact());

        mockMvc.perform(update(body("Mom", "212.555.0100", "PARENT")))
            .andExpect(status().isOk());

        verify(contactService).updateContact("contact-1", USER_ID, "Mom", "+12125550100", Relationship.PARENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+44 20 7946 0958", "123-456-7890", "555-0100", "not a phone"})
    void addContact_nonUsOrInvalidPhone_returns400WithoutEchoingIt(String phone) throws Exception {
        mockMvc.perform(add(body("Mom", phone, "PARENT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: phone"))
            .andExpect(content().string(not(containsString(phone))));

        verifyNoInteractions(contactService);
    }

    @Test
    void updateContact_invalidPhone_returns400() throws Exception {
        mockMvc.perform(update(body("Mom", "+44 20 7946 0958", "PARENT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: phone"));

        verify(contactService, never()).updateContact(any(), any(), any(), any(), any());
    }

    @Test
    void blankName_returns400() throws Exception {
        mockMvc.perform(add(body("   ", "2125550100", "PARENT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: name"));

        verifyNoInteractions(contactService);
    }

    @Test
    void nameOf100Characters_isAccepted_101IsRejected() throws Exception {
        when(contactService.addContact(anyString(), any(), any(), any())).thenReturn(new Contact());

        mockMvc.perform(add(body("n".repeat(100), "2125550100", "PARENT")))
            .andExpect(status().isOk());

        mockMvc.perform(add(body("n".repeat(101), "2125550100", "PARENT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: name"));
    }

    @Test
    void missingFields_returns400NamingAll() throws Exception {
        mockMvc.perform(add("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid fields: name, phone, relationship"));

        verifyNoInteractions(contactService);
    }

    @Test
    void unknownRelationship_returns400WithoutEchoingInput() throws Exception {
        mockMvc.perform(add(body("Mom", "2125550100", "COUSIN")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed request body"))
            .andExpect(content().string(not(containsString("2125550100"))));

        verifyNoInteractions(contactService);
    }

    @Test
    void extraFieldsAreIgnored_soAnotherUsersIdCannotBeSet() throws Exception {
        when(contactService.addContact(anyString(), any(), any(), any())).thenReturn(new Contact());

        mockMvc.perform(add("""
                {"name": "Mom", "phone": "2125550100", "relationship": "PARENT", "userId": "victim", "id": "chosen"}
                """))
            .andExpect(status().isOk());

        verify(contactService).addContact(eq(USER_ID), eq("Mom"), eq("+12125550100"), eq(Relationship.PARENT));
    }
}
