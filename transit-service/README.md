# Transit Service

## Overview

The Transit Service calculates estimated arrival windows for hubs within the LogisticsConnect system.

It combines hub information with the current transit delay stage to determine the earliest and latest estimated arrival times.

The service communicates with the Hub Service through REST and subscribes to ActiveMQ delay-stage events.

It is an independent Maven module running on port `7053`.

## Responsibilities

- Retrieve hub information from the Hub Service.
- Track delay-stage updates received through ActiveMQ.
- Calculate estimated arrival windows based on the current delay stage.
- Return transit estimates through a REST API.
- Handle requests for unknown hubs and failures involving dependent services.

## How It Works

The Transit Service combines synchronous REST requests with asynchronous messaging.

1. A client requests transit information for a hub.
2. The Transit Service retrieves the relevant hub information from the Hub Service.
3. The service determines the current delay stage.
4. It calculates the earliest and latest estimated arrival times.
5. The calculated result is returned as JSON.

The service also subscribes to ActiveMQ so that published delay-stage changes can be processed without requiring a direct REST notification from the Delay Stage Service.

## Communication Flow

```text
                   Client
                      |
                      | GET /hubs/H-501
                      v
               Transit Service
                  Port 7053
                      |
            +---------+---------+
            |                   |
            v                   |
       Hub Service              |
        Port 7051               |
            |                   |
            v                   |
     Ingestion Service          |
        Port 7050               |
                                |
     ActiveMQ Broker -----------+
    package-status-topic
            ^
            |
     Delay Stage Service
        Port 7052
```

## REST API

**Base URL:** `http://localhost:7053`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Checks whether the service is running |
| GET | `/hubs/{hubId}` | Returns transit estimates for a specified hub |

### Example Request

```http
GET http://localhost:7053/hubs/H-501
```

### Example Response

For hub `H-501` at delay stage `3`:

```json
{
  "hubId": "H-501",
  "delayStage": 3,
  "earliestArrivalMinutes": 95,
  "latestArrivalMinutes": 115
}
```

The estimated arrival window increases as the delay stage increases.

## Arrival Time Calculation

The Transit Service calculates estimated arrival windows using a baseline transit time and an additional delay determined by the current stage.

For hub `H-501`, the following results were verified during development:

| Delay Stage | Earliest Arrival | Latest Arrival |
|---|---|---|
| 0 | 50 minutes | 70 minutes |
| 3 | 95 minutes | 115 minutes |

The calculations produce an estimated arrival range rather than a single arrival time.

## ActiveMQ Integration

The Transit Service acts as a **message consumer**.

It subscribes to the ActiveMQ topic:

```text
package-status-topic
```

The Delay Stage Service publishes events containing the updated hub identifier and delay stage.

Example event:

```json
{
  "hubId": "H-501",
  "stage": 3
}
```

The Transit Service receives and processes these events to maintain the delay-stage information used for transit estimates.

The broker configuration and topic name are defined in `co.wethinkcode.logisticsconnect.mq.MqConfig`.

## Technologies

- Java
- Maven
- Javalin
- Java HTTP Client
- Jackson
- ActiveMQ Classic
- JMS
- Docker Compose

## Build and Run

### 1. Start ActiveMQ

From the LogisticsConnect project root:

```bash
docker compose -f common/docker-compose.yml up -d
```

### 2. Start Dependencies

Ensure the following services are running:

- Ingestion Service — port `7050`
- Hub Service — port `7051`
- Delay Stage Service — port `7052`

### 3. Build Transit Service

From the `transit-service` directory:

```bash
mvn clean package
```

### 4. Run Transit Service

```bash
java -jar target/transit-service.jar
```

The service starts on port `7053`.

## Testing

### Health Check

```powershell
Invoke-RestMethod http://localhost:7053/health
```

Expected response:

```text
OK
```

### Retrieve Transit Information

```powershell
Invoke-RestMethod http://localhost:7053/hubs/H-501
```

The response should contain the hub identifier, delay stage, earliest arrival estimate, and latest arrival estimate.

### Test a Delay Update

Update the delay stage through the Delay Stage Service:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:7052/delay-stage/H-501" `
  -ContentType "application/json" `
  -Body '{"stage":3}'
```

Then request the transit estimate again:

```powershell
Invoke-RestMethod http://localhost:7053/hubs/H-501
```

For the verified test scenario, the expected values are:

```json
{
  "hubId": "H-501",
  "delayStage": 3,
  "earliestArrivalMinutes": 95,
  "latestArrivalMinutes": 115
}
```

### Automated Tests

Run any configured tests using:

```bash
mvn test
```

## Design Decisions

### Separation of Responsibilities

The Transit Service focuses on calculating transit estimates rather than cleaning hub data or managing delay-stage updates.

Each responsibility is handled by its respective service.

### REST for Hub Information

REST is used to retrieve hub information because the Transit Service requires a direct response to complete a client request.

### ActiveMQ for Delay Updates

ActiveMQ allows the Transit Service to receive delay updates asynchronously.

This reduces direct dependencies between the Delay Stage Service and the Transit Service.

### Estimated Arrival Windows

The service returns both earliest and latest arrival estimates to represent a range of possible arrival times.

## Limitations

- Arrival estimates are calculated using predefined rules rather than live traffic or weather information.
- Delay-stage information is not persisted to a database.
- Hub lookups depend on the availability of upstream services.
- The service depends on ActiveMQ for receiving published delay updates.

For the complete architecture and instructions for running LogisticsConnect, see the [main project README](../README.md).