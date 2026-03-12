package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import com.campos.webscraper.shared.RetryableFetchException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for HttpJobFetcher using a mocked local HTTP transport.
 *
 * <p>Originally planned with WireMock per ADR009, but the current dependency set has a Jetty
 * incompatibility in the test runtime. The contract remains the same: mocked transport first.
 */
@Tag("unit")
@DisplayName("HttpJobFetcher")
class HttpJobFetcherTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("should fetch a successful HTML page")
    void shouldFetchSuccessfulHtmlPage() throws IOException {
        startServer(exchange -> respond(exchange, 200, "text/html; charset=utf-8", "<html><body><h1>Jobs</h1></body></html>"), "/jobs");

        JobFetcher fetcher = new HttpJobFetcher();

        FetchedPage page = fetcher.fetch(FetchRequest.of(baseUrl() + "/jobs"));

        assertThat(page.url()).isEqualTo(baseUrl() + "/jobs");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.contentType()).contains("text/html");
        assertThat(page.htmlContent()).contains("<h1>Jobs</h1>");
        assertThat(page.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("should respect followRedirects false and return redirect response")
    void shouldRespectFollowRedirectsFalseAndReturnRedirectResponse() throws IOException {
        startRedirectServer();

        JobFetcher fetcher = new HttpJobFetcher();

        FetchedPage page = fetcher.fetch(new FetchRequest(
                baseUrl() + "/redirect",
                null,
                10_000,
                false,
                null
        ));

        assertThat(page.statusCode()).isEqualTo(302);
        assertThat(page.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("should follow redirect when request allows it")
    void shouldFollowRedirectWhenRequestAllowsIt() throws IOException {
        startRedirectServer();

        JobFetcher fetcher = new HttpJobFetcher();

        FetchedPage page = fetcher.fetch(FetchRequest.of(baseUrl() + "/redirect"));

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.htmlContent()).contains("final");
        assertThat(page.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("should throw retryable exception for transient transport failure")
    void shouldThrowRetryableExceptionForTransientTransportFailure() {
        JobFetcher fetcher = new HttpJobFetcher();

        assertThatThrownBy(() -> fetcher.fetch(FetchRequest.of("http://localhost:1/unreachable")))
                .isInstanceOf(RetryableFetchException.class)
                .hasMessageContaining("Transient fetch failure");
    }

    private void startRedirectServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/final", exchange -> respond(exchange, 200, "text/html", "<html>final</html>"));
        server.start();
    }

    private void startServer(Handler handler, String path) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> handler.handle(exchange));
        server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
