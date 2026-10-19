package co.wethinkcode.logisticsconnect;

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
        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            try {
                HttpClient client = HttpClient.newHttpClient();

                HttpRequest hubRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:7051/hubs/" + hubId))
                        .GET()
                        .build();

                HttpResponse<String> hubResponse = client.send(
                        hubRequest,
                        HttpResponse.BodyHandlers.ofString()
                );
                if (hubResponse.statusCode() != 200) {
                    ctx.status(hubResponse.statusCode()).result(hubResponse.body());
                    return;
                }
                int statusCode = hubResponse.statusCode();
                String responseBody = hubResponse.body();

                System.out.println("Hub Service status: " + statusCode);
                System.out.println("Hub Service response: " + responseBody);
                ctx.status(statusCode).result(responseBody);

                HttpRequest delayRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:7052/delay-stage/" + hubId))
                        .GET()
                        .build();

                HttpResponse<String> delayResponse = client.send(
                        delayRequest,
                        HttpResponse.BodyHandlers.ofString()
                );
                if (delayResponse.statusCode() != 200) {
                    ctx.status(502).result("Failed to retrieve delay stage");
                    return;
                }
                ObjectMapper mapper = new ObjectMapper();
                JsonNode delayData = mapper.readTree(delayResponse.body());
                int stage = delayData.get("stage").asInt();
                int baseMinutes = 60;
                int delayMinutes = stage * 15;

                int estimatedMinutes = baseMinutes + delayMinutes;

                int earliestMinutes = estimatedMinutes - 10;
                int latestMinutes = estimatedMinutes + 10;
                ctx.json(Map.of(
                        "hubId", hubId,
                        "delayStage", stage,
                        "earliestArrivalMinutes", earliestMinutes,
                        "latestArrivalMinutes", latestMinutes
                ));
            }catch (IOException e) {
                ctx.status(502).result("Dependent service unavailable");
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(503).result("Request interrupted");
            }
    });
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
