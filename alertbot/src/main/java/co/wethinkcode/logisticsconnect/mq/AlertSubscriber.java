
package co.wethinkcode.logisticsconnect.mq;

import co.wethinkcode.logisticsconnect.AlertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

public class AlertSubscriber {

    private final AlertService alertService = new AlertService();
    private final ObjectMapper mapper = new ObjectMapper();

    public void start() throws Exception {

        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(MqConfig.BROKER_URL);

        Connection connection = factory.createConnection();

        Session session = connection.createSession(
                false, Session.AUTO_ACKNOWLEDGE);

        MessageConsumer consumer = session.createConsumer(
                session.createTopic(MqConfig.TOPIC));

        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String json = ((TextMessage) message).getText();
                    JsonNode event = mapper.readTree(json);

                    String hubId = event.path("hubId").asText();
                    int stage = event.path("stage").asInt();

                    alertService.checkDelay(hubId, stage);
                }
            } catch (Exception e) {
                System.err.println("Alert error: " + e.getMessage());
            }
        });

        connection.start();

        System.out.println("AlertBot subscribed to " + MqConfig.TOPIC);
    }
}
