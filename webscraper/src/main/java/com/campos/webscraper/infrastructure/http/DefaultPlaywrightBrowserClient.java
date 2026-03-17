package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.RetryableFetchException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.PlaywrightException;

import java.util.Objects;

/**
 * Default Playwright-backed client that drives Chromium for dynamic pages.
 */
public final class DefaultPlaywrightBrowserClient implements PlaywrightBrowserClient {

    private final BrowserType.LaunchOptions launchOptions;

    public DefaultPlaywrightBrowserClient() {
        this(new BrowserType.LaunchOptions().setHeadless(true));
    }

    DefaultPlaywrightBrowserClient(BrowserType.LaunchOptions launchOptions) {
        this.launchOptions = Objects.requireNonNull(launchOptions, "launchOptions must not be null");
    }

    @Override
    public PlaywrightBrowserResponse fetch(FetchRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(launchOptions);
             BrowserContext context = browser.newContext()) {
            try (Page page = context.newPage()) {
                if (!request.headers().isEmpty()) {
                    page.setExtraHTTPHeaders(request.headers());
                }
                page.setDefaultNavigationTimeout(request.timeoutMs());
                page.setDefaultTimeout(request.timeoutMs());
                Response response = page.navigate(request.url(), new Page.NavigateOptions()
                        .setTimeout((double) request.timeoutMs()));

                String html = (response == null || response.body() == null) ? "" : response.text();
                String contentType = (response == null) ? null : response.headers().get("content-type");
                int status = (response == null) ? 0 : response.status();

                return new PlaywrightBrowserResponse(
                        page.url(),
                        html,
                        status,
                        contentType
                );
            }
        } catch (PlaywrightException exception) {
            throw new RetryableFetchException("Playwright fetch failed for " + request.url(), exception);
        }
    }
}
