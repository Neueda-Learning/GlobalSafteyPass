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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@AutoConfigureMockMvc
class ReadinessControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTestHelper.login(mvc, json, "Jessie Han");
    }

    @Test
    void readinessCheckAndGet() throws Exception {
        mvc.perform(post("/api/travel/trips/trip-paris/readiness-check").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.checks").isArray());
        mvc.perform(get("/api/travel/trips/trip-paris/readiness").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("trip-paris"));
    }

    @Test
    void currencyFallbackEndpoint() throws Exception {
        mvc.perform(post("/api/travel/trips/trip-paris/readiness/currency-fallback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"option\":\"USD_SETTLEMENT\"}"))
                .andExpect(status().isOk());
    }
}
