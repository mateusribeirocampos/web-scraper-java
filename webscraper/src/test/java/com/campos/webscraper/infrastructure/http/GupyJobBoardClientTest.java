package com.campos.webscraper.infrastructure.http;

import com.campos.webscraper.interfaces.dto.GupyJobListingResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("GupyJobBoardClient")
class GupyJobBoardClientTest {

    @Test
    @DisplayName("should keep only jobs from the requested career page and city when base url encodes board identity")
    void shouldKeepOnlyJobsFromTheRequestedCareerPageAndCityWhenBaseUrlEncodesBoardIdentity() {
        OkHttpClient client = clientResponding(200, """
                {
                  "data": [
                    {
                      "id": 8672422,
                      "name": "Banco de Talentos - Extrema/MG",
                      "careerPageName": "Special Dog Company",
                      "publishedDate": "2025-02-18T18:44:29.394Z",
                      "applicationDeadline": null,
                      "isRemoteWork": false,
                      "city": "Extrema",
                      "state": "Minas Gerais",
                      "country": "Brasil",
                      "jobUrl": "https://specialdogcompany.gupy.io/job/8672422",
                      "workplaceType": "on-site",
                      "type": "vacancy_type_talent_pool"
                    },
                    {
                      "id": 9006170,
                      "name": "Auxiliar de Produção",
                      "careerPageName": "Special Dog Company",
                      "publishedDate": "2025-04-22T21:02:25.260Z",
                      "applicationDeadline": null,
                      "isRemoteWork": false,
                      "city": "Pouso Alegre",
                      "state": "Minas Gerais",
                      "country": "Brasil",
                      "jobUrl": "https://specialdogcompany.gupy.io/job/9006170",
                      "workplaceType": "on-site",
                      "type": "vacancy_type_talent_pool"
                    },
                    {
                      "id": 9006188,
                      "name": "Auxiliar Operações - Extrema",
                      "careerPageName": "Diversidade Comfrio",
                      "publishedDate": "2025-04-22T21:02:25.260Z",
                      "applicationDeadline": null,
                      "isRemoteWork": false,
                      "city": "Extrema",
                      "state": "Minas Gerais",
                      "country": "Brasil",
                      "jobUrl": "https://diversidadecomfrio.gupy.io/job/9006188",
                      "workplaceType": "on-site",
                      "type": "vacancy_type_talent_pool"
                    }
                  ],
                  "pagination": {
                    "offset": 0,
                    "limit": 100,
                    "total": 3
                  }
                }
                """);
        GupyJobBoardClient gupyJobBoardClient = new GupyJobBoardClient(client, new com.fasterxml.jackson.databind.ObjectMapper());

        List<GupyJobListingResponse> response = gupyJobBoardClient.fetchAllJobs(
                "https://portal.api.gupy.io/api/v1/jobs?careerPageName=Special%20Dog%20Company&city=Extrema"
        );

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(8672422L);
        assertThat(response.getFirst().careerPageName()).isEqualTo("Special Dog Company");
        assertThat(response.getFirst().city()).isEqualTo("Extrema");
    }

    @Test
    @DisplayName("should throw descriptive exception when Gupy returns non-success status")
    void shouldThrowDescriptiveExceptionWhenGupyReturnsNonSuccessStatus() {
        OkHttpClient client = clientResponding(503, "{\"error\":\"unavailable\"}");
        GupyJobBoardClient gupyJobBoardClient = new GupyJobBoardClient(client, new com.fasterxml.jackson.databind.ObjectMapper());

        assertThatThrownBy(() -> gupyJobBoardClient.fetchAllJobs(
                "https://portal.api.gupy.io/api/v1/jobs?jobName=Extrema"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gupy API request failed")
                .hasMessageContaining("503");
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
