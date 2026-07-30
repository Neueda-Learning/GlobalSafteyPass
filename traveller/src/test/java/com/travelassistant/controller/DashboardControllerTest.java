package com.travelassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelassistant.support.AuthTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@AutoConfigureMockMvc
class DashboardControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTestHelper.login(mvc, json, "Jessie Han");
    }

    @Test
    void dashboardAndExchangeRate() throws Exception {
        mvc.perform(get("/api/travel/trips/trip-paris/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("trip-paris"))
                .andExpect(jsonPath("$.budget").value(3200));
        mvc.perform(get("/api/travel/trips/trip-paris/exchange-rate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("trip-paris"));
    }
}
