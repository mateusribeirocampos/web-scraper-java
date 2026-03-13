package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.GreenhouseJobBoardItemResponse;
import com.campos.webscraper.interfaces.dto.LeverPostingResponse;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("AtsJobProviderClient contract")
class AtsJobProviderClientContractTest {

    @Test
    @DisplayName("should allow Greenhouse and Lever clients to share the same provider contract")
    void shouldAllowGreenhouseAndLeverClientsToShareTheSameProviderContract() {
        AtsJobProviderClient<GreenhouseJobBoardItemResponse> greenhouseClient = new GreenhouseJobBoardClient(
                clientResponding(200, "{\"jobs\":[]}")
        );
        AtsJobProviderClient<LeverPostingResponse> leverClient = new LeverPostingsClient(
                clientResponding(200, "[]")
        );

        List<GreenhouseJobBoardItemResponse> greenhouseJobs = greenhouseClient.fetchPublishedJobs(
                "https://boards-api.greenhouse.io/v1/boards/bitso/jobs?content=true"
        );
        List<LeverPostingResponse> leverJobs = leverClient.fetchPublishedJobs(
                "https://api.lever.co/v0/postings/example"
        );

        assertThat(greenhouseJobs).isEmpty();
        assertThat(leverJobs).isEmpty();
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
}
