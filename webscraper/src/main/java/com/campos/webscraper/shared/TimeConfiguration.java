package com.campos.webscraper.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Shared time source to keep scheduling and tests deterministic.
 */
@Configuration
public class TimeConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
