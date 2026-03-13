package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.IndeedApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Objects;

/**
 * HTTP client for the Indeed MCP connector.
 */
@Component
public class IndeedApiClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public IndeedApiClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public IndeedApiClient(OkHttpClient httpClient) {
        this(httpClient, new ObjectMapper());
    }

    public IndeedApiClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Fetches a job payload from the Indeed MCP endpoint and deserializes the JSON response.
     */
    public IndeedApiResponse fetchJob(String url) {
        Objects.requireNonNull(url, "url must not be null");

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Indeed MCP request failed with status " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Indeed MCP response body is missing");
            }

            return objectMapper.readValue(body.string(), IndeedApiResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Indeed MCP request failed: " + exception.getMessage(), exception);
        }
    }
}
