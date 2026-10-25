package co.wethinkcode.logisticsconnect.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class DelayStagePublisher {

    private final ActiveMQConnectionFactory connectionFactory;

    public DelayStagePublisher() {
        this.connectionFactory =
                new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
    }

    public void publish(String hubId, int stage) throws JMSException {

        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {
            connection = connectionFactory.createConnection();

            session = connection.createSession(
                    false,
                    Session.AUTO_ACKNOWLEDGE
            );

            Topic topic = session.createTopic(MqConfig.TOPIC);

            producer = session.createProducer(topic);

            String payload = String.format(
                    "{\"hubId\":\"%s\",\"stage\":%d}",
                    hubId,
                    stage
            );

            TextMessage message = session.createTextMessage(payload);

            producer.send(message);

            System.out.println("Published delay stage update: " + payload);

        } finally {
            try {
                if (producer != null) {
                    producer.close();
                }
            } finally {
                try {
                    if (session != null) {
                        session.close();
                    }
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
            }
        }
    }
}
