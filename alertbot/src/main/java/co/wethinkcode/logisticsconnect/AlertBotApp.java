
package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.AlertSubscriber;
import io.javalin.Javalin;

public class AlertBotApp {

    public static void main(String[] args) throws Exception {

        AlertSubscriber subscriber = new AlertSubscriber();
        subscriber.start();

        Javalin app = Javalin.create().start(7054);

        app.get("/health", ctx -> ctx.result("OK"));
    }
}
