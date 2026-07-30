package com.travelassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelassistant.support.AuthTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@AutoConfigureMockMvc
class AuthenticationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void authFlowAndSession() throws Exception {
        String token = AuthTestHelper.login(mvc, json, "Jessie Han");
        mvc.perform(get("/api/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-001"));
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mvc.perform(get("/api/customer/profile"))
                .andExpect(status().isUnauthorized());
    }
}
