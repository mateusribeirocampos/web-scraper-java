package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.shared.RetryableFetchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("PlaywrightJobFetcher")
class PlaywrightJobFetcherTest {

    @Test
    @DisplayName("should map Playwright response into a FetchedPage")
    void shouldMapPlaywrightResponseIntoFetchedPage() {
        Instant now = Instant.parse("2026-03-17T16:30:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        PlaywrightBrowserClient stubClient = request -> new PlaywrightBrowserClient.PlaywrightBrowserResponse(
                "https://dynamic.example.com",
                "<html><body>rendered</body></html>",
                200,
                "text/html"
        );

        PlaywrightJobFetcher fetcher = new PlaywrightJobFetcher(stubClient, fixedClock);

        FetchedPage page = fetcher.fetch(FetchRequest.of("https://dynamic.example.com"));

        assertThat(page.url()).isEqualTo("https://dynamic.example.com");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.htmlContent()).contains("rendered");
        assertThat(page.contentType()).isEqualTo("text/html");
        assertThat(page.fetchedAt()).isEqualTo(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        assertThat(page.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("should wrap runtime errors as RetryableFetchException")
    void shouldWrapRuntimeErrorsAsRetryableFetchException() {
        PlaywrightBrowserClient explodingClient = request -> {
            throw new IllegalStateException("browser crashed");
        };
        PlaywrightJobFetcher fetcher = new PlaywrightJobFetcher(explodingClient, Clock.systemUTC());

        assertThatThrownBy(() -> fetcher.fetch(FetchRequest.of("https://dynamic.example.com")))
                .isInstanceOf(RetryableFetchException.class)
                .hasMessageContaining("Playwright");
    }
}
