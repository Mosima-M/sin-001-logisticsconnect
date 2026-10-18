package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransitServiceApp {

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7051/hubs/" +hubId))
                .GET()
                .build();

            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
            int statusCode = response.statusCode();
            String responseBody = response.body();

            System.out.println("Hub Service status: " + statusCode);
            System.out.println("Hub Service response: " + responseBody);
            ctx.status(statusCode).result(responseBody);
    });
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
