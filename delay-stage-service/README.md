# Delay Stage Service

## Overview

The Delay Stage Service manages transit delay stages for hubs within the LogisticsConnect system.

Each hub can have a delay stage ranging from `0` to `8`, representing different levels of transit disruption.

The service provides REST endpoints for retrieving and updating delay stages. When a stage is updated, it publishes an event to an ActiveMQ topic so that other services can react asynchronously.

The service is an independent Maven module running on port `7052`.

## Responsibilities

- Store and retrieve the current delay stage for each hub.
- Return a default stage of `0` for hubs without a recorded delay.
- Accept delay-stage updates through a REST endpoint.
- Validate delay-stage values.
- Publish delay updates to the ActiveMQ topic `package-status-topic`.
- Allow multiple subscribers to process delay events independently.

## How It Works

The service combines synchronous REST communication with asynchronous messaging.

1. A client sends a request to update a hub's delay stage.
2. The service processes the update and stores the new stage.
3. A message containing the hub identifier and updated stage is published to ActiveMQ.
4. Subscribed services receive the event and process it independently.

### Communication Flow

```text
               Client
                  |
                  | POST /delay-stage/H-501
                  v
         Delay Stage Service
              Port 7052
                  |
          Update delay stage
                  |
                  v
           ActiveMQ Broker
         package-status-topic
                  |
          +-------+-------+
          |               |
          v               v
    Transit Service     AlertBot
       Port 7053        Port 7054
```

## REST API

**Base URL:** `http://localhost:7052`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Check whether the service is running |
| GET | `/delay-stage/{hubId}` | Retrieve the current delay stage for a hub |
| POST | `/delay-stage/{hubId}` | Update a hub's delay stage |

### Retrieve a Delay Stage

```http
GET http://localhost:7052/delay-stage/H-501
```

If no delay stage has been recorded for the hub, the service returns the default value of `0`.

### Update a Delay Stage

```http
POST http://localhost:7052/delay-stage/H-501
Content-Type: application/json
```

Request body:

```json
{
  "stage": 5
}
```

The service updates the delay stage and publishes an event to ActiveMQ.

### Validation

Delay stages must be within the supported range of `0` to `8`.

Invalid requests should be rejected rather than stored as valid delay updates.

## ActiveMQ Integration

The service acts as a **message producer**.

When a delay stage changes, the service publishes an event to the ActiveMQ topic:

```text
package-status-topic
```

The event contains the information needed by subscribers:

```json
{
  "hubId": "H-501",
  "stage": 5
}
```

### Topic-Based Messaging

ActiveMQ uses a publish-subscribe model for these events.

- **Delay Stage Service:** Publishes delay-stage events.
- **Transit Service:** Subscribes to delay-stage events for transit processing.
- **AlertBot:** Subscribes to delay-stage events and checks whether an alert is required.

The broker URL and topic name are defined in `co.wethinkcode.logisticsconnect.mq.MqConfig`.

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

### 2. Build the Service

From the `delay-stage-service` directory:

```bash
mvn clean package
```

### 3. Run the Service

```bash
java -jar target/delay-stage-service.jar
```

The service starts on port `7052`.

## Testing

### Health Check

```powershell
Invoke-RestMethod http://localhost:7052/health
```

Expected response:

```text
OK
```

### Retrieve a Delay Stage

```powershell
Invoke-RestMethod http://localhost:7052/delay-stage/H-501
```

### Update a Delay Stage

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:7052/delay-stage/H-501" `
  -ContentType "application/json" `
  -Body '{"stage":5}'
```

### Verify Message Publishing

With ActiveMQ, Transit Service, and AlertBot running:

1. Send a POST request setting the stage to `5`.
2. Confirm the request is processed successfully.
3. Check the AlertBot terminal for the simulated alert.

Example AlertBot output:

```text
ALERT: Hub H-501 has severe delays. Stage: 5
```

This verifies that a delay-stage update can trigger downstream processing through ActiveMQ.

Run any configured automated tests using:

```bash
mvn test
```

## Design Decisions

### In-Memory Delay Tracking

Delay stages are maintained in memory rather than stored in a database.

This simplifies the implementation but means delay-stage data is not persistent across application restarts.

### REST for Direct Requests

REST provides a straightforward way for clients to retrieve or update a specific hub's delay stage.

### ActiveMQ for Event Notifications

ActiveMQ allows multiple services to receive delay-stage updates without the Delay Stage Service having to call each subscriber directly.

This reduces direct dependencies between the publisher and its subscribers.

## Limitations

- Delay-stage values are not persisted to a database.
- The service requires a running ActiveMQ broker for message publishing.
- Topic subscribers must be connected to receive ordinary non-persistent topic notifications; the current implementation does not establish durable subscriptions.
- The system does not provide historical delay-stage records.

For the overall system architecture and startup instructions, see the [main LogisticsConnect README](../README.md).
