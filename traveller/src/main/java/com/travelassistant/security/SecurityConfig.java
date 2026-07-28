package com.travelassistant.security;

import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http,AuthTokenFilter filter)throws Exception{
        return http.csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/styles.css","/app.js","/api/auth/**",
                        "/api/public/exchange-rates/**","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/h2-console/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Secure authentication is required.\"}");}))
                .headers(h->h.frameOptions(f->f.sameOrigin())).build();
    }
}
