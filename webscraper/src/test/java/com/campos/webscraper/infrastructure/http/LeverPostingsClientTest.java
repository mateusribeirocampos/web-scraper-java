package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("LeverPostingsClient")
class LeverPostingsClientTest {

    @Test
    @DisplayName("should deserialize published jobs from the Lever postings API")
    void shouldDeserializePublishedJobsFromTheLeverPostingsApi() throws IOException {
        OkHttpClient client = clientResponding(200, fixture("fixtures/lever/lever-postings-response.json"));
        LeverPostingsClient leverPostingsClient = new LeverPostingsClient(client);

        List<LeverPostingResponse> response = leverPostingsClient.fetchPublishedJobs(
                "https://api.lever.co/v0/postings/example"
        );

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().id()).isEqualTo("lever-1");
        assertThat(response.getFirst().text()).isEqualTo("Senior Java Engineer");
        assertThat(response.getFirst().categories().location()).isEqualTo("Sao Paulo");
        assertThat(response.getFirst().hostedUrl()).isEqualTo("https://jobs.example.com/lever-1");
        assertThat(response.getFirst().description()).contains("Java");
    }

    @Test
    @DisplayName("should throw descriptive exception when Lever returns non-success status")
    void shouldThrowDescriptiveExceptionWhenLeverReturnsNonSuccessStatus() {
        OkHttpClient client = clientResponding(429, "{\"error\":\"rate limited\"}");
        LeverPostingsClient leverPostingsClient = new LeverPostingsClient(client);

        assertThatThrownBy(() -> leverPostingsClient.fetchPublishedJobs(
                "https://api.lever.co/v0/postings/example"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lever postings request failed")
                .hasMessageContaining("429");
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
        try (InputStream inputStream = LeverPostingsClientTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
