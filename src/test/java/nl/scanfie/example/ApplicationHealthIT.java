package nl.scanfie.example;

import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationHealthIT extends AbstractIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    public void shouldStartApplicationInTomcatWithPostgressAndRedis() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isLessThan(500);
    }
}