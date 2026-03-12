package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.IndeedApiResponse;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for IndeedApiClient.
 *
 * TDD RED: written before the client exists.
 */
@Tag("unit")
@DisplayName("IndeedApiClient")
class IndeedApiClientTest {

    @Test
    @DisplayName("should deserialize a successful JSON response from Indeed MCP")
    void shouldDeserializeSuccessfulJsonResponse() throws IOException {
        OkHttpClient client = clientResponding(200, fixture("fixtures/indeed/indeed-job-response.json"));
        IndeedApiClient indeedApiClient = new IndeedApiClient(client);

        IndeedApiResponse response = indeedApiClient.fetchJob("https://to.indeed.test/api/jobs/123");

        assertThat(response.jobId()).isEqualTo("5-cmh1-0-1jj9snbbvr8er800-358c3bd3a6b73ba5");
        assertThat(response.title()).isEqualTo("Java Backend Developer | Jr (Remote)");
        assertThat(response.company()).isEqualTo("Invillia");
        assertThat(response.location()).isEqualTo("Remoto");
        assertThat(response.postedAt()).isEqualTo("2026-03-05");
        assertThat(response.applyUrl()).isEqualTo("https://to.indeed.com/aas2tpyk2v6d");
    }

    @Test
    @DisplayName("should throw descriptive exception when Indeed MCP returns non-success status")
    void shouldThrowDescriptiveExceptionWhenNonSuccessStatus() {
        OkHttpClient client = clientResponding(503, "{\"error\":\"unavailable\"}");
        IndeedApiClient indeedApiClient = new IndeedApiClient(client);

        assertThatThrownBy(() -> indeedApiClient.fetchJob("https://to.indeed.test/api/jobs/123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Indeed MCP request failed")
                .hasMessageContaining("503");
    }

    private static OkHttpClient clientResponding(int statusCode, String body) {
        Interceptor interceptor = chain -> {
            Request request = chain.request();
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message("mock")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .build();
        };

        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
    }

    private static String fixture(String classpathLocation) throws IOException {
        try (InputStream inputStream = IndeedApiClientTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
