package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.shared.FetchRequest;
import com.campos.webscraper.shared.FetchedPage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * OkHttp-based implementation of JobFetcher for static HTTP retrieval.
 */
public class HttpJobFetcher implements JobFetcher {

    @Override
    public FetchedPage fetch(FetchRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(request.timeoutMs()))
                .readTimeout(Duration.ofMillis(request.timeoutMs()))
                .followRedirects(request.followRedirects())
                .followSslRedirects(request.followRedirects())
                .build();

        Request.Builder requestBuilder = new Request.Builder().url(request.url());
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            ResponseBody body = response.body();
            String content = body == null ? "" : body.string();
            String contentType = body == null || body.contentType() == null
                    ? null
                    : body.contentType().toString();

            return new FetchedPage(
                    request.url(),
                    content,
                    response.code(),
                    contentType,
                    LocalDateTime.now()
            );
        } catch (IOException exception) {
            return new FetchedPage(
                    request.url(),
                    "",
                    599,
                    null,
                    LocalDateTime.now()
            );
        }
    }
}
