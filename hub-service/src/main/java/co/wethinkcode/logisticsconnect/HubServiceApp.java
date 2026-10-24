package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;

public class HubServiceApp {

    public static void main(String[] args) {

        IngestionClient ingestionClient = new IngestionClient();
        HubLookupService hubLookupService = new HubLookupService(ingestionClient);
        Javalin app = Javalin.create();

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");

            try {
                JsonNode hub = hubLookupService.findHubById(hubId);

                if (hub == null) {
                    ctx.status(404).result("Hub not found");
                    return;
                }

                ctx.json(hub);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(502).result("Hub lookup interrupted");

            } catch (Exception e) {
                ctx.status(502).result("Ingestion Service unavailable");
            }
        });
        app.start(7051);
    }
}
