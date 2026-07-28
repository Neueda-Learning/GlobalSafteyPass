package com.travelassistant.controller;

import com.travelassistant.security.*;
import com.travelassistant.security.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService service;
    public AuthenticationController(AuthenticationService service){this.service=service;}
    @PostMapping("/start") public StartResponse start(@Valid @RequestBody StartRequest request){return service.start(request.customerId());}
    @PostMapping("/sms") public SmsResponse sms(@RequestParam String challengeId){return service.sendSms(challengeId);}
    @PostMapping("/verify") public VerifyResponse verify(@Valid @RequestBody VerifyRequest request){return service.verify(request);}
    @GetMapping("/session") public SessionResponse session(@RequestHeader("Authorization")String authorization){
        var s=service.requireSession(authorization.substring(7));return new SessionResponse(s.customerId(),s.method(),s.expiresAt());
    }
    @PostMapping("/logout") public void logout(@RequestHeader("Authorization")String authorization){service.logout(authorization.substring(7));}
    @PostMapping("/step-up") public StepUpResponse stepUp(@Valid @RequestBody StepUpRequest request,
            @RequestHeader("Authorization")String authorization){
        var session=service.requireSession(authorization.substring(7));return service.stepUp(session.customerId(),request);
    }
}
