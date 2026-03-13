package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * HTTP client for Lever public postings endpoints.
 */
public class LeverPostingsClient implements AtsJobProviderClient<LeverPostingResponse> {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LeverPostingsClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public LeverPostingsClient(OkHttpClient httpClient) {
        this(httpClient, new ObjectMapper());
    }

    public LeverPostingsClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public List<LeverPostingResponse> fetchPublishedJobs(String url) {
        Objects.requireNonNull(url, "url must not be null");

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Lever postings request failed with status " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Lever postings response body is missing");
            }

            LeverPostingResponse[] payload = objectMapper.readValue(body.string(), LeverPostingResponse[].class);
            if (payload == null) {
                return List.of();
            }

            return List.of(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Lever postings request failed: " + exception.getMessage(), exception);
        }
    }
}
