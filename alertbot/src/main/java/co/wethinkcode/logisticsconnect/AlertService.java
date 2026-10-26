package co.wethinkcode.logisticsconnect;

public class AlertService {

    public void checkDelay(String hubId, int stage) {
        if (stage >= 4) {
            System.out.println("ALERT: Hub " + hubId
                    + " has severe delays. Stage: " + stage);
        }
    }
}
