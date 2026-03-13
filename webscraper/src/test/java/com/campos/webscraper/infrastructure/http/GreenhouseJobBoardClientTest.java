package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
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
@DisplayName("GreenhouseJobBoardClient")
class GreenhouseJobBoardClientTest {

    @Test
    @DisplayName("should deserialize published jobs from the Greenhouse Job Board API")
    void shouldDeserializePublishedJobsFromTheGreenhouseJobBoardApi() throws IOException {
        OkHttpClient client = clientResponding(200, fixture("fixtures/greenhouse/greenhouse-bitso-jobs-response.json"));
        GreenhouseJobBoardClient greenhouseJobBoardClient = new GreenhouseJobBoardClient(client);

        List<GreenhouseJobBoardItemResponse> response = greenhouseJobBoardClient.fetchPublishedJobs(
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true"
        );

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().id()).isEqualTo(6120911003L);
        assertThat(response.getFirst().title()).isEqualTo("Senior Java Engineer");
        assertThat(response.getFirst().companyName()).isEqualTo("Bitso");
        assertThat(response.getFirst().location().name()).isEqualTo("Latin America");
        assertThat(response.getFirst().absoluteUrl()).isEqualTo("https://bitso.com/jobs/6120911003?gh_jid=6120911003");
        assertThat(response.getFirst().firstPublished()).isEqualTo("2024-09-13T11:35:49-04:00");
        assertThat(response.getFirst().content()).contains("Java");
    }

    @Test
    @DisplayName("should throw descriptive exception when Greenhouse Job Board returns non-success status")
    void shouldThrowDescriptiveExceptionWhenGreenhouseJobBoardReturnsNonSuccessStatus() {
        OkHttpClient client = clientResponding(503, "{\"error\":\"unavailable\"}");
        GreenhouseJobBoardClient greenhouseJobBoardClient = new GreenhouseJobBoardClient(client);

        assertThatThrownBy(() -> greenhouseJobBoardClient.fetchPublishedJobs(
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Greenhouse Job Board request failed")
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
        try (InputStream inputStream = GreenhouseJobBoardClientTest.class.getClassLoader()
                .getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
