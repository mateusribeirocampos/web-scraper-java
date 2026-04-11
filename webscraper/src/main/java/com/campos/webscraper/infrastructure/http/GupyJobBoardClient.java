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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        return filterByBoardIdentity(baseUrl, all);
    }

    private List<GupyJobListingResponse> filterByBoardIdentity(String baseUrl, List<GupyJobListingResponse> items) {
        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null) {
            return items;
        }

        String expectedCareerPageName = Optional.ofNullable(parsed.queryParameter("careerPageName"))
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .orElse(null);
        String expectedCity = Optional.ofNullable(parsed.queryParameter("city"))
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .orElse(null);

        return items.stream()
                .filter(item -> matchesCareerPageName(item, expectedCareerPageName))
                .filter(item -> matchesCity(item, expectedCity))
                .toList();
    }

    private boolean matchesCareerPageName(GupyJobListingResponse item, String expectedCareerPageName) {
        if (expectedCareerPageName == null) {
            return true;
        }
        return expectedCareerPageName.equalsIgnoreCase(
                item.careerPageName() == null ? "" : item.careerPageName().strip());
    }

    private boolean matchesCity(GupyJobListingResponse item, String expectedCity) {
        if (expectedCity == null) {
            return true;
        }
        return normalizeFacet(expectedCity).equals(normalizeFacet(item.city()));
    }

    private String normalizeFacet(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
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
