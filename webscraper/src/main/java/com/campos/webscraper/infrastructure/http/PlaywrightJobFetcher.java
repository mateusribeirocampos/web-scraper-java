package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.shared.RetryableFetchException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * {@link JobFetcher} implementation backed by Playwright for JS-heavy (Tipo C) sites.
 */
public final class PlaywrightJobFetcher implements JobFetcher {

    private final PlaywrightBrowserClient browserClient;
    private final Clock clock;

    public PlaywrightJobFetcher() {
        this(new DefaultPlaywrightBrowserClient(), Clock.systemUTC());
    }

    public PlaywrightJobFetcher(PlaywrightBrowserClient browserClient, Clock clock) {
        this.browserClient = Objects.requireNonNull(browserClient, "browserClient must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public FetchedPage fetch(FetchRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        PlaywrightBrowserClient.PlaywrightBrowserResponse response;
        try {
            response = browserClient.fetch(request);
        } catch (RetryableFetchException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RetryableFetchException("Playwright fetch failed: " + request.url(), exception);
        }

        LocalDateTime fetchedAt = LocalDateTime.now(clock);
        return new FetchedPage(
                response.url(),
                response.htmlContent(),
                response.statusCode(),
                response.contentType(),
                fetchedAt
        );
    }
}
