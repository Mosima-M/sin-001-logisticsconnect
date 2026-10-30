# AlertBot Service

## Overview

AlertBot is the notification service in the LogisticsConnect system. It monitors transit delay-stage updates and generates simulated alerts when a hub experiences significant delays.

The service subscribes to an ActiveMQ topic and processes incoming events asynchronously.

AlertBot is an independent Maven module running on port `7054`.

Notifications are simulated through console output. The implementation does not post to actual social media platforms or external webhooks.

## Responsibilities

- Subscribe to the ActiveMQ topic `package-status-topic`.
- Receive delay-stage events published by the Delay Stage Service.
- Extract the hub identifier and delay stage from each message.
- Evaluate the delay stage against a predefined threshold.
- Generate a simulated alert when the delay stage is `4` or higher.
- Provide a health-check endpoint.

## How It Works

AlertBot uses event-driven communication rather than repeatedly requesting delay information through REST.

1. A client updates a hub's delay stage through the Delay Stage Service.
2. The Delay Stage Service publishes an event to ActiveMQ.
3. AlertBot receives the event through its JMS message listener.
4. The message is parsed to extract the hub identifier and delay stage.
5. AlertBot checks whether the stage meets the alert threshold.
6. If the stage is `4` or higher, a simulated alert is printed to the console.

## Communication Flow

```text
              Client
                 |
                 | POST /delay-stage/H-501
                 v
        Delay Stage Service
             Port 7052
                 |
                 | Publish event
                 v
          ActiveMQ Broker
        package-status-topic
                 |
                 | Subscribe
                 v
              AlertBot
             Port 7054
                 |
          Check stage >= 4
                 |
          +------+------+
          |             |
         Yes            No
          |             |
     Print alert     No alert
```

## ActiveMQ Integration

AlertBot acts as a message consumer.

It subscribes to:

```text
package-status-topic
```

The broker URL and topic name are configured through `co.wethinkcode.logisticsconnect.mq.MqConfig`.

### Example Message

```json
{
  "hubId": "H-501",
  "stage": 5
}
```

The `AlertSubscriber` component receives the message, uses Jackson to parse its JSON content, and passes the extracted values to `AlertService`.

### Alert Threshold

The alerting rule is implemented in `AlertService`.

| Delay Stage | Behaviour |
|---|---|
| 0–3 | No alert generated |
| 4–8 | Simulated alert generated |

### Example Alert

For hub `H-501` at delay stage `5`:

```text
ALERT: Hub H-501 has severe delays. Stage: 5
```

This output was verified during development.

## REST API

**Base URL:** `http://localhost:7054`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Returns `OK` when AlertBot is running |

AlertBot does not expose an endpoint for manually triggering notifications. Alerts are generated from incoming ActiveMQ events.

## Technologies

- Java
- Maven
- Javalin
- ActiveMQ Classic
- JMS
- Jackson
- Docker Compose

## Build and Run

### 1. Start ActiveMQ

From the LogisticsConnect project root:

```bash
docker compose -f common/docker-compose.yml up -d
```

### 2. Build AlertBot

From the `alertbot` directory:

```bash
mvn clean package
```

### 3. Run AlertBot

```bash
java -jar target/alertbot.jar
```

AlertBot starts on port `7054` and subscribes to the ActiveMQ topic.

## Testing

### Health Check

```powershell
Invoke-RestMethod http://localhost:7054/health
```

Expected response:

```text
OK
```

### Test the Alerting Flow

Ensure that ActiveMQ, the Delay Stage Service, and AlertBot are running.

Send a delay-stage update:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:7052/delay-stage/H-501" `
  -ContentType "application/json" `
  -Body '{"stage":5}'
```

Check the AlertBot terminal for:

```text
ALERT: Hub H-501 has severe delays. Stage: 5
```

### Test Below the Threshold

Send another update:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:7052/delay-stage/H-501" `
  -ContentType "application/json" `
  -Body '{"stage":2}'
```

No alert should be generated because the delay stage is below `4`.

### Automated Tests

Any configured automated tests can be run with:

```bash
mvn test
```

## Design Decisions

### Event-Driven Notifications

AlertBot subscribes to ActiveMQ instead of polling the Delay Stage Service for updates.

This allows alerts to be triggered when messages arrive.

### Separation of Responsibilities

`AlertSubscriber` handles messaging and JSON parsing, while `AlertService` handles the alert threshold and notification logic.

This keeps message consumption separate from the alerting rules.

### Simulated Notifications

Console output is used instead of integrating with an external social media platform.

This demonstrates the alerting workflow without requiring third-party API credentials or external services.

## Limitations

- Notifications are printed to the console rather than delivered to actual users.
- Alerts are not stored in a database.
- There is no implemented retry or notification history mechanism.
- The current subscription does not provide durable delivery for messages published while AlertBot is disconnected.

For the complete system architecture, see the [main LogisticsConnect README](../README.md).