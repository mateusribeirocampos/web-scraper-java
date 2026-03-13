package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * HTTP client for the public Greenhouse Job Board API.
 */
public class GreenhouseJobBoardClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GreenhouseJobBoardClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public GreenhouseJobBoardClient(OkHttpClient httpClient) {
        this(httpClient, new ObjectMapper());
    }

    public GreenhouseJobBoardClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Fetches published jobs from a public Greenhouse board endpoint.
     */
    public List<GreenhouseJobBoardItemResponse> fetchPublishedJobs(String url) {
        Objects.requireNonNull(url, "url must not be null");

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Greenhouse Job Board request failed with status " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Greenhouse Job Board response body is missing");
            }

            GreenhouseJobBoardResponse payload = objectMapper.readValue(body.string(), GreenhouseJobBoardResponse.class);
            if (payload.jobs() == null) {
                return List.of();
            }

            return payload.jobs();
        } catch (IOException exception) {
            throw new IllegalStateException("Greenhouse Job Board request failed: " + exception.getMessage(), exception);
        }
    }
}
