package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.DouApiItemResponse;
import com.campos.webscraper.interfaces.dto.DouApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * HTTP client for the DOU open-data API.
 */
public class DouApiClient {

    private static final List<String> KEYWORDS = List.of(
            "analista de ti",
            "desenvolvedor",
            "tecnologia da informacao"
    );

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DouApiClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public DouApiClient(OkHttpClient httpClient) {
        this(httpClient, new ObjectMapper());
    }

    public DouApiClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Fetches DOU notices and keeps only the records matching TI-related keywords.
     */
    public List<DouApiItemResponse> searchRelevantNotices(String url) {
        Objects.requireNonNull(url, "url must not be null");

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("DOU API request failed with status " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("DOU API response body is missing");
            }

            DouApiResponse payload = objectMapper.readValue(body.string(), DouApiResponse.class);
            if (payload.items() == null) {
                return List.of();
            }

            return payload.items().stream()
                    .filter(this::matchesRelevantKeywords)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("DOU API request failed: " + exception.getMessage(), exception);
        }
    }

    private boolean matchesRelevantKeywords(DouApiItemResponse item) {
        String haystack = ((item.title() == null ? "" : item.title()) + " "
                + (item.summary() == null ? "" : item.summary()))
                .toLowerCase(Locale.ROOT);

        return KEYWORDS.stream().anyMatch(haystack::contains);
    }
}
