package com.travelassistant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties={"integration.fx.enabled=false","spring.profiles.active=test"})
class TravelAssistantApplicationTests {
    @Test void contextLoads() {}
}
