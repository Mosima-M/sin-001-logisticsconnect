package co.wethinkcode.logisticsconnect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelayStageService {

    private final Map<String, Integer> delayStages =
            new ConcurrentHashMap<>();

    public int getStage(String hubId) {
        return delayStages.getOrDefault(
                hubId.toUpperCase(), 0
        );
    }

    public void updateStage(String hubId, int stage) {
        if (stage < 0 || stage > 8) {
            throw new IllegalArgumentException(
                    "Stage must be between 0 and 8"
            );
        }

        delayStages.put(hubId.toUpperCase(), stage);
    }
}
