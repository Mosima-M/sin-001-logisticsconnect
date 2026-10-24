package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.JsonNode;

public class HubLookupService {

    private final IngestionClient ingestionClient;

    public HubLookupService(IngestionClient ingestionClient) {
        this.ingestionClient = ingestionClient;
    }

    public JsonNode findHubById(String hubId) throws Exception {
        JsonNode hubs = ingestionClient.fetchHubs();

        for (JsonNode hub : hubs) {
            if (hubId.equalsIgnoreCase(hub.path("hubId").asText())) {
                return hub;
            }
        }

        return null;
    }
}
