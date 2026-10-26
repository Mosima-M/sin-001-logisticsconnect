
package co.wethinkcode.logisticsconnect.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelayStageSubscriber implements AutoCloseable {

    private final Map<String, Integer> latestStages =
            new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    public void start() throws JMSException {

        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(MqConfig.BROKER_URL);

        connection = factory.createConnection();

        session = connection.createSession(
                false,
                Session.AUTO_ACKNOWLEDGE
        );

        Topic topic = session.createTopic(MqConfig.TOPIC);

        consumer = session.createConsumer(topic);

        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {

                    String json = ((TextMessage) message).getText();

                    JsonNode data = mapper.readTree(json);

                    String hubId = data.get("hubId").asText();
                    int stage = data.get("stage").asInt();

                    latestStages.put(hubId, stage);

                    System.out.println(
                            "Received delay stage update: " + json
                    );
                }

            } catch (Exception e) {
                System.err.println(
                        "Failed to process delay stage message: "
                                + e.getMessage()
                );
            }
        });

        connection.start();

        System.out.println(
                "Subscribed to ActiveMQ topic: " + MqConfig.TOPIC
        );
    }

    public int getStage(String hubId) {
        return latestStages.getOrDefault(hubId, 0);
    }

    @Override
    public void close() throws JMSException {
        try {
            if (consumer != null) consumer.close();
        } finally {
            try {
                if (session != null) session.close();
            } finally {
                if (connection != null) connection.close();
            }
        }
    }
}

