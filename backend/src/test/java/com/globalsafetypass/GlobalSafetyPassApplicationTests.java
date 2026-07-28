package com.globalsafetypass;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalSafetyPassApplicationTests {
    @Autowired MockMvc mockMvc;

    @Test
    void contextLoads() {}

    @Test
    void rejectsTripWhenReturnDateIsNotAfterDeparture() throws Exception {
        mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "destinationCountry": "Japan",
                      "destinationCity": "Tokyo",
                      "startDate": "2026-08-10",
                      "endDate": "2026-08-10",
                      "budget": 2500,
                      "currency": "JPY",
                      "cardId": 1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Return date must be after the departure date."));
    }
}
