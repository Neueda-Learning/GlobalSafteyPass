package com.travelassistant.controller;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.AtmMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AtmMapControllerTest {
    @Mock AtmMapService maps;
    @Mock CustomerContext customer;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AtmMapController(maps, customer)).build();
    }

    @Test
    void geocodeEndpointReturnsLocation() throws Exception {
        when(customer.customerId()).thenReturn("customer-001");
        when(maps.geocode("Paris")).thenReturn(new MapLocationResponse("Paris", "Paris, France", 48.8566, 2.3522));

        mockMvc.perform(get("/api/travel/maps/geocode").param("q", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(48.8566))
                .andExpect(jsonPath("$.longitude").value(2.3522))
                .andExpect(jsonPath("$.label").value("Paris, France"));

        verify(customer).customerId();
        verify(maps).geocode("Paris");
    }

    @Test
    void atmsEndpointReturnsNearbyResults() throws Exception {
        when(customer.customerId()).thenReturn("customer-001");
        var atm = new AtmLocationResponse("node-1", "HSBC ATM", "HSBC", "1 Main St", "24/7",
                35.6762, 139.6503, 120);
        when(maps.nearby(35.6762, 139.6503, 2000))
                .thenReturn(new AtmSearchResponse(35.6762, 139.6503, 2000, List.of(atm), "OpenStreetMap"));

        mockMvc.perform(get("/api/travel/maps/atms")
                        .param("lat", "35.6762").param("lon", "139.6503").param("radius", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atms[0].name").value("HSBC ATM"))
                .andExpect(jsonPath("$.atms[0].distanceMeters").value(120))
                .andExpect(jsonPath("$.provider").value("OpenStreetMap"));
    }

    @Test
    void atmsEndpointUsesDefaultRadius() throws Exception {
        when(customer.customerId()).thenReturn("customer-001");
        when(maps.nearby(anyDouble(), anyDouble(), eq(2000)))
                .thenReturn(new AtmSearchResponse(0, 0, 2000, List.of(), "OpenStreetMap"));

        mockMvc.perform(get("/api/travel/maps/atms").param("lat", "0").param("lon", "0"))
                .andExpect(status().isOk());

        verify(maps).nearby(0, 0, 2000);
    }
}
