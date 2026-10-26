
package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.DelayStageSubscriber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class TransitServiceApp {

    public static void main(String[] args) throws Exception {

        DelayStageSubscriber subscriber = new DelayStageSubscriber();
        subscriber.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                subscriber.close();
            } catch (Exception e) {
                System.err.println(
                        "Failed to close subscriber: " + e.getMessage()
                );
            }
        }));

        Javalin app = Javalin.create().start(7053);

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/hubs/{hubId}", ctx -> {

            String hubId = ctx.pathParam("hubId").toUpperCase();

            try {
                HttpRequest hubRequest = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:7051/hubs/" + hubId
                        ))
                        .GET()
                        .build();

                HttpResponse<String> hubResponse = client.send(
                        hubRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (hubResponse.statusCode() != 200) {
                    ctx.status(hubResponse.statusCode())
                            .result(hubResponse.body());
                    return;
                }

                JsonNode hubData = mapper.readTree(hubResponse.body());

                // Use the latest delay stage received from ActiveMQ
                int stage = subscriber.getStage(hubId);

                int[] arrivalWindow = EtaCalculator.calculate(stage);

                ctx.json(Map.of(
                        "hubId", hubId,
                        "delayStage", stage,
                        "earliestArrivalMinutes", arrivalWindow[0],
                        "latestArrivalMinutes", arrivalWindow[1]
                ));

            } catch (IOException e) {
                ctx.status(502).result(
                        "Dependent service unavailable"
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(503).result("Request interrupted");
            }
        });
    }
}


// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
