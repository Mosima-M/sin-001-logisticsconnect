package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HubServiceApp {

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs/{hubId}", ctx -> {

            String hubId = ctx.pathParam("hubId");

            try {
                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:7050/hubs"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() != 200) {
                    ctx.status(502).result("Failed to retrieve hubs");
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode hubs = mapper.readTree(response.body());

                for (JsonNode hub : hubs) {
                    if (hubId.equalsIgnoreCase(hub.path("hubId").asText())) {
                        ctx.json(hub);
                        return;
                    }
                }

                ctx.status(404).result("Hub not found");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(502).result("Hub lookup interrupted");

            } catch (Exception e) {
                ctx.status(502).result("Ingestion Service unavailable");
            }
        });
    }
}
