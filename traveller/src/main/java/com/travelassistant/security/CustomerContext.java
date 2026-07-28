package com.travelassistant.security;

import com.travelassistant.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CustomerContext {
    private final HttpServletRequest request;
    public CustomerContext(HttpServletRequest request) { this.request = request; }
    public String customerId() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null||!authentication.isAuthenticated()||authentication.getPrincipal()==null)
            throw new UnauthorizedException("Secure authentication is required.");
        return authentication.getPrincipal().toString();
    }
}
