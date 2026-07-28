package com.travelassistant.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

@Configuration
public class WebClientConfig {
    @Bean
    WebClient fxWebClient(@Value("${integration.fx.base-url}") String baseUrl,
                          @Value("${integration.fx.timeout-seconds:3}") int timeout) {
        HttpClient client = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout * 1000)
                .responseTimeout(Duration.ofSeconds(timeout))
                .doOnConnected(c -> c.addHandlerLast(new ReadTimeoutHandler(timeout))
                        .addHandlerLast(new WriteTimeoutHandler(timeout)));
        return WebClient.builder().baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(client)).build();
    }
    @Bean
    WebClient fraudWebClient(@Value("${integration.fraud.base-url:http://localhost}") String baseUrl) {
        HttpClient client=HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS,3000)
                .responseTimeout(Duration.ofSeconds(3));
        return WebClient.builder().baseUrl(baseUrl).clientConnector(new ReactorClientHttpConnector(client)).build();
    }
    @Bean
    WebClient mapGeocodingWebClient(@Value("${integration.maps.geocoding-base-url}") String baseUrl,
            @Value("${integration.maps.timeout-seconds:12}") int timeout) {
        HttpClient client=HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS,timeout*1000)
                .responseTimeout(Duration.ofSeconds(timeout));
        return WebClient.builder().baseUrl(baseUrl).defaultHeader("User-Agent","VoyageTravelAssistant/1.0")
                .clientConnector(new ReactorClientHttpConnector(client)).build();
    }
    @Bean
    WebClient mapPlacesWebClient(@Value("${integration.maps.places-base-url}") String baseUrl,
            @Value("${integration.maps.places-timeout-seconds:7}") int timeout) {
        HttpClient client=HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS,timeout*1000)
                .responseTimeout(Duration.ofSeconds(timeout));
        return WebClient.builder().baseUrl(baseUrl).defaultHeader("User-Agent","VoyageTravelAssistant/1.0")
                .clientConnector(new ReactorClientHttpConnector(client)).build();
    }
}
