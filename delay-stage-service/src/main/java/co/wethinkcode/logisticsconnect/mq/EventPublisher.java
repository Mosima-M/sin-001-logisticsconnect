package co.wethinkcode.logisticsconnect.mq;

public interface EventPublisher {
    void publish(String hubId, int stage) throws Exception;
}
