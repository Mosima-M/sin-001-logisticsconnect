package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayStageServiceAppTest {

    private Javalin app;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        app = DelayStageServiceApp.createApp().start(0);
        client = HttpClient.newHttpClient();

        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void tearDown() {
        app.stop();
        client.close();
    }
}
