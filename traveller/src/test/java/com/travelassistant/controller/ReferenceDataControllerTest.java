package com.travelassistant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@AutoConfigureMockMvc
class ReferenceDataControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void publicReferenceEndpointsDoNotRequireAuth() throws Exception {
        mvc.perform(get("/api/public/reference/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/public/reference/cities").param("country", "Japan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/public/reference/currencies").param("country", "France"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void publicExchangeRateEndpoint() throws Exception {
        mvc.perform(get("/api/public/exchange-rates/live").param("base", "USD").param("quote", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").isNumber());
    }
}
