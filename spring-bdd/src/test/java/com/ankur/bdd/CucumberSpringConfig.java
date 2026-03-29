package com.ankur.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

/**
 * Bridges Cucumber and Spring Boot.
 *
 * @CucumberContextConfiguration — tells cucumber-spring: use this class
 *                                  to bootstrap the Spring ApplicationContext.
 * @SpringBootTest               — starts the full Spring Boot context so that
 *                                  step definitions can use @Autowired beans
 *                                  (e.g. RestTemplate, @Value properties).
 */
@CucumberContextConfiguration
@SpringBootTest
@Import(CucumberSpringConfig.Beans.class)
public class CucumberSpringConfig {

    @TestConfiguration
    static class Beans {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}