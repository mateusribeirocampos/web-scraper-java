package com.campos.webscraper.config;

import com.campos.webscraper.infrastructure.http.PlaywrightJobFetcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes Playwright-specific beans required by iteration 11.
 */
@Configuration
public class PlaywrightConfiguration {

    @Bean
    public PlaywrightJobFetcher playwrightJobFetcher() {
        return new PlaywrightJobFetcher();
    }
}
