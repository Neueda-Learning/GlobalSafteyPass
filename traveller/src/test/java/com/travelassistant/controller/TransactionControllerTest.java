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

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"integration.fx.enabled=false", "spring.profiles.active=test"})
@AutoConfigureMockMvc
class TransactionControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTestHelper.login(mvc, json, "Jessie Han");
    }

    @Test
    void listTransactionsAndFailureExplanation() throws Exception {
        mvc.perform(get("/api/travel/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].transactionId", hasItem("txn-declined")));
        mvc.perform(get("/api/travel/transactions/txn-declined/failure-explanation").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Payment limit reached"));
    }

    @Test
    void receiveTransactionEvent() throws Exception {
        String id = "txn-api-" + UUID.randomUUID();
        mvc.perform(post("/api/travel/transactions/events").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"%s","cardId":"card-001","merchantName":"Cafe","merchantCountry":"France",
                                "merchantCity":"Paris","merchantCategory":"DINING","originalAmount":25.00,"originalCurrency":"EUR",
                                "transactionTime":"2026-11-06T14:00:00Z","transactionType":"PURCHASE","status":"APPROVED"}"""
                                .formatted(id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(id));
    }

    @Test
    void recoveryEndpoint() throws Exception {
        mvc.perform(get("/api/travel/transactions/txn-declined/recovery").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-declined"));
    }
}
