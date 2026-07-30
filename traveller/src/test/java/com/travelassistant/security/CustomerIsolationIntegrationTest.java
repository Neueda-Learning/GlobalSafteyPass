package com.travelassistant.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"integration.fx.enabled=false","spring.profiles.active=test"})
@AutoConfigureMockMvc
class CustomerIsolationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void knownCustomersReceiveOnlyTheirOwnProfileAndCards() throws Exception {
        String jessieToken=login("Jessie Han");
        String millyToken=login("Milly Li");

        mvc.perform(get("/api/customer/profile").header("Authorization","Bearer "+jessieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-001"))
                .andExpect(jsonPath("$.displayName").value("Jessie Han"));
        mvc.perform(get("/api/travel/cards").header("Authorization","Bearer "+jessieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id",hasItem("card-001")))
                .andExpect(jsonPath("$.data[*].id",not(hasItem("card-201"))));

        mvc.perform(get("/api/customer/profile").header("Authorization","Bearer "+millyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-002"))
                .andExpect(jsonPath("$.displayName").value("Milly Li"));
        mvc.perform(get("/api/travel/cards").header("Authorization","Bearer "+millyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id",hasItem("card-201")))
                .andExpect(jsonPath("$.data[*].id",not(hasItem("card-001"))));
        mvc.perform(get("/api/travel/trips/trip-paris").header("Authorization","Bearer "+millyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Trip does not belong to current customer."));
    }

    @Test
    void unknownCustomerCannotStartAuthentication() throws Exception {
        mvc.perform(post("/api/auth/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Unknown Person\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("We could not find a banking profile for that name."));
    }

    private String login(String displayName) throws Exception {
        String start=mvc.perform(post("/api/auth/start").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new NameRequest(displayName))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String challengeId=json.readTree(start).get("challengeId").asText();
        String verify=mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new VerifyRequest(challengeId,"TRUSTED_DEVICE","trusted-device-demo"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode response=json.readTree(verify);
        return response.get("accessToken").asText();
    }

    private record NameRequest(String displayName){}
    private record VerifyRequest(String challengeId,String method,String credential){}
}
