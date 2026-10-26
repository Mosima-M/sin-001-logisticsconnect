
package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.DelayStagePublisher;
import co.wethinkcode.logisticsconnect.mq.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.util.Map;

public class DelayStageServiceApp {

    public static void main(String[] args) {
        createApp().start(7052);
    }

    // Production uses the real ActiveMQ publisher
    public static Javalin createApp() {
        return createApp(new DelayStagePublisher());
    }

    // Allows us to inject a different publisher for testing
    public static Javalin createApp(EventPublisher publisher) {

        Javalin app = Javalin.create();

        DelayStageService delayStageService = new DelayStageService();
        ObjectMapper mapper = new ObjectMapper();

        // Health endpoint
        app.get("/health", ctx -> ctx.result("OK"));

        // Get delay stage for a hub
        app.get("/delay-stage/{hubId}", ctx -> {

            String hubId = ctx.pathParam("hubId").toUpperCase();

            int stage = delayStageService.getStage(hubId);

            ctx.json(Map.of(
                    "hubId", hubId,
                    "stage", stage
            ));
        });

        // Update delay stage for a hub
        app.post("/delay-stage/{hubId}", ctx -> {

            String hubId = ctx.pathParam("hubId").toUpperCase();
            String requestBody = ctx.body();

            try {
                JsonNode newStage = mapper.readTree(requestBody);

                if (newStage == null || !newStage.has("stage")) {
                    ctx.status(400).result("Missing stage field");
                    return;
                }

                if (!newStage.get("stage").isIntegralNumber()) {
                    ctx.status(400).result("Stage must be an Integer");
                    return;
                }

                int stage = newStage.get("stage").asInt();

                try {
                    delayStageService.updateStage(hubId, stage);

                    publisher.publish(hubId, stage);

                    ctx.json(Map.of(
                            "hubId", hubId,
                            "stage", stage
                    ));

                } catch (IllegalArgumentException e) {
                    ctx.status(400).result(e.getMessage());

                } catch (Exception e) {
                    ctx.status(503)
                            .result("Unable to publish delay stage update");
                }

            } catch (JsonProcessingException e) {
                ctx.status(400).result("Invalid Json");
            }
        });

        return app;
    }
}
