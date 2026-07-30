package com.travelassistant.security;

import com.travelassistant.exception.UnauthorizedException;
import com.travelassistant.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static com.travelassistant.security.AuthDtos.*;

@Service
public class AuthenticationService {
    private static final Duration CHALLENGE_TTL=Duration.ofMinutes(5),SESSION_TTL=Duration.ofMinutes(20);
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Challenge> challenges=new ConcurrentHashMap<>();
    private final Map<String,Session> sessions=new ConcurrentHashMap<>();
    private final Map<String,StepUpGrant> stepUpGrants=new ConcurrentHashMap<>();
    private final CustomerRepository customers;

    public AuthenticationService(CustomerRepository customers){this.customers=customers;}

    public StartResponse start(String displayName){
        String normalizedName=displayName==null?"":displayName.trim().replaceAll("\\s+"," ");
        String customerId=customers.findByDisplayNameIgnoreCase(normalizedName).map(c->c.getId())
                .orElseThrow(()->new UnauthorizedException("We could not find a banking profile for that name."));
        String id=UUID.randomUUID().toString();Instant expires=Instant.now().plus(CHALLENGE_TTL);
        challenges.put(id,new Challenge(customerId,expires,0,false));
        return new StartResponse(id,Method.TRUSTED_DEVICE,List.of(Method.TRUSTED_DEVICE,Method.APP_PIN,Method.SMS_OTP),
                "••• ••• 0188",expires,"Use this trusted device first. SMS remains available if you prefer it.");
    }
    public SmsResponse sendSms(String challengeId){
        Challenge c=challenge(challengeId);challenges.put(challengeId,new Challenge(c.customerId,c.expiresAt,c.attempts,true));
        return new SmsResponse(challengeId,"••• ••• 0188",c.expiresAt,"246810");
    }
    public VerifyResponse verify(VerifyRequest request){
        Challenge c=challenge(request.challengeId());
        boolean valid=switch(request.method()){
            case TRUSTED_DEVICE -> "trusted-device-demo".equals(request.credential());
            case APP_PIN -> "2580".equals(request.credential());
            case SMS_OTP -> c.smsSent&&"246810".equals(request.credential());
        };
        if(!valid){
            challenges.put(request.challengeId(),new Challenge(c.customerId,c.expiresAt,c.attempts+1,c.smsSent));
            throw new UnauthorizedException("Verification was not accepted. Please try again.");
        }
        challenges.remove(request.challengeId());String token=randomToken();Instant expires=Instant.now().plus(SESSION_TTL);
        sessions.put(token,new Session(c.customerId,request.method(),expires));
        return new VerifyResponse(token,c.customerId,request.method(),expires,"Bearer");
    }
    public Session requireSession(String token){
        Session s=sessions.get(token);
        if(s==null||s.expiresAt.isBefore(Instant.now())){sessions.remove(token);throw new UnauthorizedException("Your secure session has expired.");}
        return s;
    }
    public void logout(String token){sessions.remove(token);}
    public StepUpResponse stepUp(String customerId,StepUpRequest request){
        boolean valid=switch(request.method()){
            case TRUSTED_DEVICE -> "trusted-device-demo".equals(request.credential());
            case APP_PIN -> "2580".equals(request.credential());
            case SMS_OTP -> "246810".equals(request.credential());
        };
        if(!valid)throw new UnauthorizedException("Additional verification was not accepted.");
        String token=randomToken();Instant expires=Instant.now().plus(Duration.ofMinutes(2));
        stepUpGrants.put(token,new StepUpGrant(customerId,request.action(),request.resourceId(),expires));
        return new StepUpResponse(token,request.action(),request.resourceId(),expires);
    }
    public void consumeStepUp(String token,String customerId,String action,String resourceId){
        if(token==null||token.isBlank())throw new UnauthorizedException("Additional verification is required for this card action.");
        StepUpGrant grant=stepUpGrants.remove(token);
        if(grant==null||grant.expiresAt.isBefore(Instant.now())||!grant.customerId.equals(customerId)
                ||!grant.action.equals(action)||!grant.resourceId.equals(resourceId))
            throw new UnauthorizedException("Additional verification is required for this card action.");
    }
    private Challenge challenge(String id){
        Challenge c=challenges.get(id);
        if(c==null||c.expiresAt.isBefore(Instant.now()))throw new UnauthorizedException("Verification request has expired.");
        if(c.attempts>=5)throw new UnauthorizedException("Too many attempts. Start a new verification.");
        return c;
    }
    private String randomToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private record Challenge(String customerId,Instant expiresAt,int attempts,boolean smsSent){}
    public record Session(String customerId,Method method,Instant expiresAt){}
    private record StepUpGrant(String customerId,String action,String resourceId,Instant expiresAt){}
}
