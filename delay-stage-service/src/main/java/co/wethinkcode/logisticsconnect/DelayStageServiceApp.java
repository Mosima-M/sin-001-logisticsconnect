package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelayStageServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7052);
        Map<String, Integer> delayStages = new ConcurrentHashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/delay-stage/{hubId}", ctx -> {

            String hubId = ctx.pathParam("hubId").toUpperCase();

            int stage = delayStages.getOrDefault(hubId, 0);

            ctx.json(Map.of(
                    "hubId", hubId,
                    "stage", stage
            ));
        });
        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();
            String requestBody = ctx.body();
            try {
                JsonNode newStage = mapper.readTree(requestBody);

                if (!newStage.has("stage")) {
                    ctx.status(400).result("Missing stage field");
                    return;
                }
                if (!newStage.get("stage").isIntegralNumber()) {
                    ctx.status(400).result("Stage must be an Integer");
                    return;
                }
                int stage = newStage.get("stage").asInt();
                if (stage >= 0 && stage <= 8) {
                    delayStages.put(hubId, stage);
                    ctx.json(Map.of("hubId", hubId, "stage", stage));
                } else {
                    ctx.status(400).result("Stage must be between 0 and 8");
                }
            }
            catch(JsonProcessingException e){
                ctx.status(400).result("Invalid Json");
            }
        });
    }
}


