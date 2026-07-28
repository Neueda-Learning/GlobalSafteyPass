package com.travelassistant.controller;

import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.security.CustomerContext;
import com.travelassistant.service.SupportCaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/travel/cases") @Tag(name="Case tracking")
public class SupportCaseController {
    private final SupportCaseService service;private final CustomerContext customer;
    public SupportCaseController(SupportCaseService service,CustomerContext customer){this.service=service;this.customer=customer;}
    @GetMapping public ListResponse<SupportCaseResponse> list(){var data=service.list(customer.customerId());return new ListResponse<>(data,data.size());}
}
