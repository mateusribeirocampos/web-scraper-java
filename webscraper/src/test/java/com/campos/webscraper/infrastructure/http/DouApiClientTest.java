package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
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

/**
 * Unit tests for DouApiClient.
 *
 * TDD RED: written before the client exists.
 */
@Tag("unit")
@DisplayName("DouApiClient")
class DouApiClientTest {

    @Test
    @DisplayName("should deserialize DOU JSON and keep only configured TI keywords")
    void shouldDeserializeDouJsonAndKeepOnlyConfiguredTiKeywords() throws IOException {
        OkHttpClient client = clientResponding(200, fixture("fixtures/dou/dou-response.json"));
        DouApiClient douApiClient = new DouApiClient(client);

        List<DouApiItemResponse> items = douApiClient.searchRelevantNotices("https://in.gov.br/api/dou");

        assertThat(items).hasSize(2);
        assertThat(items).extracting(DouApiItemResponse::title)
                .containsExactly(
                        "Analista de TI - Desenvolvimento de Sistemas",
                        "Desenvolvedor Backend Java"
                );
    }

    @Test
    @DisplayName("should throw descriptive exception when DOU API returns non-success status")
    void shouldThrowDescriptiveExceptionWhenNonSuccessStatus() {
        OkHttpClient client = clientResponding(502, "{\"error\":\"bad gateway\"}");
        DouApiClient douApiClient = new DouApiClient(client);

        assertThatThrownBy(() -> douApiClient.searchRelevantNotices("https://in.gov.br/api/dou"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOU API request failed")
                .hasMessageContaining("502");
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
        try (InputStream inputStream = DouApiClientTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IOException("Fixture not found: " + classpathLocation);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
