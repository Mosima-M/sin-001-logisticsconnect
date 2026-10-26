
package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.EventPublisher;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayStageServiceAppTest {

    private Javalin app;
    private HttpClient client;
    private String baseUrl;
    private FakePublisher publisher;

    // Fake implementation: no ActiveMQ connection needed
    private static class FakePublisher implements EventPublisher {

        private String lastHubId;
        private int lastStage;
        private int publishCount;

        @Override
        public void publish(String hubId, int stage) {
            lastHubId = hubId;
            lastStage = stage;
            publishCount++;
        }
    }

    @BeforeEach
    void setUp() {
        publisher = new FakePublisher();

        app = DelayStageServiceApp.createApp(publisher).start(0);
        client = HttpClient.newHttpClient();

        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void tearDown() {
        app.stop();
        client.close();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void unknownHubDefaultsToStageZero() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/delay-stage/H-501"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"stage\":0"));
    }

    @Test
    void validPostUpdatesDelayStage() throws Exception {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/delay-stage/H-501"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"stage\":3}"))
                .build();

        HttpResponse<String> postResponse = client.send(
                postRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, postResponse.statusCode());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/delay-stage/H-501"))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(
                getRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"stage\":3"));

        // Verify that the event was published
        assertEquals(1, publisher.publishCount);
        assertEquals("H-501", publisher.lastHubId);
        assertEquals(3, publisher.lastStage);
    }

    @Test
    void rejectsStageOutsideValidRange() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/delay-stage/H-501"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"stage\":9}"))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        assertEquals("Stage must be between 0 and 8", response.body());

        assertEquals(0, publisher.publishCount);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/delay-stage/H-501"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"stage\":}"))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        assertEquals("Invalid Json", response.body());

        assertEquals(0, publisher.publishCount);
    }

    @Test
    void rejectsNonIntegerStage() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/delay-stage/H-501"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"stage\":\"three\"}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        assertEquals("Stage must be an Integer", response.body());

        assertEquals(0, publisher.publishCount);
    }
}

