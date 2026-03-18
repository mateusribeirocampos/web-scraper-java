package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.GupyJobListingResponse;
import com.campos.webscraper.interfaces.dto.GupyJobPageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HTTP client for the public Gupy Portal API.
 *
 * <p>Fetches all pages for a given search URL, handling pagination automatically.
 * The base URL stored in {@code target_sites.base_url} must be a valid Gupy portal
 * search endpoint, e.g.:
 * {@code https://portal.api.gupy.io/api/v1/jobs?jobName=java+backend&limit=100}
 */
@Component
public class GupyJobBoardClient {

    static final int PAGE_SIZE = 100;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GupyJobBoardClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public GupyJobBoardClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Fetches all published jobs matching the query embedded in {@code baseUrl},
     * iterating over all pages until exhausted.
     *
     * @param baseUrl full Gupy portal search URL (may include query params)
     * @return flat list of all matching job listings
     */
    public List<GupyJobListingResponse> fetchAllJobs(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");

        List<GupyJobListingResponse> all = new ArrayList<>();
        int offset = 0;

        while (true) {
            GupyJobPageResponse page = fetchPage(baseUrl, offset);

            List<GupyJobListingResponse> items = page.data();
            if (items == null || items.isEmpty()) {
                break;
            }

            all.addAll(items);

            int total = page.pagination() != null ? page.pagination().total() : 0;
            offset += items.size();

            if (offset >= total) {
                break;
            }
        }

        return all;
    }

    private GupyJobPageResponse fetchPage(String baseUrl, int offset) {
        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid Gupy base URL: " + baseUrl);
        }

        HttpUrl url = parsed.newBuilder()
                .setQueryParameter("limit", String.valueOf(PAGE_SIZE))
                .setQueryParameter("offset", String.valueOf(offset))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException(
                        "Gupy API request failed with status " + response.code() + " for URL: " + url);
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Gupy API response body is missing for URL: " + url);
            }

            return objectMapper.readValue(body.string(), GupyJobPageResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Gupy API request failed: " + exception.getMessage(), exception);
        }
    }
}
