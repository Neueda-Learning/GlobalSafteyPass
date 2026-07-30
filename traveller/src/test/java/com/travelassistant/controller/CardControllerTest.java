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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@AutoConfigureMockMvc
class CardControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTestHelper.login(mvc, json, "Jessie Han");
    }

    @Test
    void listCardsForCustomer() throws Exception {
        mvc.perform(get("/api/travel/cards").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem("card-001")))
                .andExpect(jsonPath("$.data[*].id", not(hasItem("card-201"))));
    }

    @Test
    void enableOverseasPayments() throws Exception {
        mvc.perform(post("/api/travel/cards/card-002/enable-overseas-payments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overseasPaymentsEnabled").value(true));
    }

    @Test
    void freezeRequiresStepUp() throws Exception {
        mvc.perform(post("/api/travel/cards/card-002/freeze").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        String stepUp = AuthTestHelper.stepUp(mvc, json, token, "CARD_FREEZE", "card-002");
        mvc.perform(post("/api/travel/cards/card-002/freeze")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Step-Up-Token", stepUp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }
}
