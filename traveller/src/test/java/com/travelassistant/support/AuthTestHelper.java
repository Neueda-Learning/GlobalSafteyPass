package com.travelassistant.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestHelper {
    private AuthTestHelper() {}

    public static String login(MockMvc mvc, ObjectMapper json, String displayName) throws Exception {
        String start = mvc.perform(post("/api/auth/start").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new NameRequest(displayName))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String challengeId = json.readTree(start).get("challengeId").asText();
        String verify = mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new VerifyRequest(challengeId, "TRUSTED_DEVICE", "trusted-device-demo"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode response = json.readTree(verify);
        return response.get("accessToken").asText();
    }

    public static String stepUp(MockMvc mvc, ObjectMapper json, String bearer, String action, String resourceId) throws Exception {
        String body = mvc.perform(post("/api/auth/step-up").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .content(json.writeValueAsString(new StepUpRequest(action, resourceId, "TRUSTED_DEVICE", "trusted-device-demo"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("stepUpToken").asText();
    }

    private record NameRequest(String displayName) {}
    private record VerifyRequest(String challengeId, String method, String credential) {}
    private record StepUpRequest(String action, String resourceId, String method, String credential) {}
}
