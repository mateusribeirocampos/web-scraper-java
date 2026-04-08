package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.WorkdayJobPostingResponse;
import com.campos.webscraper.interfaces.dto.WorkdayJobsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP client for public Workday job boards exposed through the CXS endpoint.
 */
@Component
public class WorkdayJobBoardClient {

    static final int PAGE_SIZE = 20;
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WorkdayJobBoardClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public WorkdayJobBoardClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public List<WorkdayJobPostingResponse> fetchJobsByLocation(String apiUrl, String locationFacetId) {
        Objects.requireNonNull(apiUrl, "apiUrl must not be null");
        Objects.requireNonNull(locationFacetId, "locationFacetId must not be null");

        List<WorkdayJobPostingResponse> all = new ArrayList<>();
        int offset = 0;

        while (true) {
            WorkdayJobsResponse page = fetchPage(apiUrl, locationFacetId, offset);
            List<WorkdayJobPostingResponse> items = page.jobPostings();
            if (items == null || items.isEmpty()) {
                break;
            }

            all.addAll(items);
            offset += items.size();
            if (offset >= page.total()) {
                break;
            }
        }

        return all;
    }

    private WorkdayJobsResponse fetchPage(String apiUrl, String locationFacetId, int offset) {
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(serializeRequestBody(locationFacetId, offset), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException(
                        "Workday jobs request failed with status " + response.code() + " for URL: " + apiUrl
                );
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Workday jobs response body is missing for URL: " + apiUrl);
            }

            return objectMapper.readValue(body.string(), WorkdayJobsResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Workday jobs request failed: " + exception.getMessage(), exception);
        }
    }

    private String serializeRequestBody(String locationFacetId, int offset) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appliedFacets", Map.of("locations", List.of(locationFacetId)));
        body.put("limit", PAGE_SIZE);
        body.put("offset", offset);
        body.put("searchText", "");
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Workday jobs request body", exception);
        }
    }
}
