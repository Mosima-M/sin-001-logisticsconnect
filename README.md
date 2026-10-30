# LogisticsConnect

## Overview

LogisticsConnect is a Java-based distributed system designed to manage parcel delivery hubs, track transit delays, and calculate estimated arrival windows.

The project demonstrates how independent services communicate using both synchronous REST APIs and asynchronous messaging through ActiveMQ.

It was developed in four stages:

1. **Data Ingestion:** Read, clean, and expose legacy hub data from a CSV file.
2. **REST Integration:** Connect independent services through HTTP APIs to retrieve hub information and calculate arrival estimates.
3. **Message Queue Integration:** Introduce ActiveMQ to publish and consume delay-stage events asynchronously.
4. **AlertBot:** Generate simulated notifications when transit delays reach a predefined threshold.

All four stages have been implemented and manually verified.

## System Architecture

LogisticsConnect consists of five independent Java services and an ActiveMQ message broker.

```text
                    CLIENT
                      |
          +-----------+-----------+
          |                       |
          v                       v
    Transit Service        Delay Stage Service
       Port 7053                Port 7052
          |                       |
          | REST                  | Publishes events
          v                       v
     Hub Service             ActiveMQ Broker
       Port 7051          package-status-topic
          |                       |
          | REST            +-----+------+
          v                 |            |
   Ingestion Service        v            v
       Port 7050      Transit Service  AlertBot
                         Port 7053     Port 7054
```

### Communication Methods

**Synchronous REST communication**

REST is used when a service needs an immediate response from another service.

- Hub Service requests cleaned hub records from Ingestion Service.
- Transit Service retrieves hub information from Hub Service.
- Clients use REST endpoints to retrieve hub information, update delay stages, and request arrival estimates.

**Asynchronous messaging**

ActiveMQ is used to distribute delay-stage updates without requiring direct HTTP calls between the publisher and subscribers.

- Delay Stage Service publishes delay updates to `package-status-topic`.
- Transit Service subscribes to the topic to receive updated delay stages.
- AlertBot subscribes to the same topic to evaluate whether an alert should be generated.

This demonstrates the difference between request-response communication and event-driven messaging.

## Services

| Service | Port | Responsibility |
|---|---|---|
| [Ingestion Service](ingestion-service/README.md) | 7050 | Reads and cleans hub records from CSV |
| [Hub Service](hub-service/README.md) | 7051 | Retrieves hub information through REST |
| [Delay Stage Service](delay-stage-service/README.md) | 7052 | Manages delay stages and publishes ActiveMQ events |
| [Transit Service](transit-service/README.md) | 7053 | Calculates estimated arrival windows |
| [AlertBot](alertbot/README.md) | 7054 | Generates simulated alerts for severe delays |

The [`common/`](common/) directory contains the Docker Compose configuration and documentation for the ActiveMQ broker.

Each service is an independent Maven module with its own `pom.xml`. The repository does not use a parent Maven project.

## Technologies

| Technology | Purpose |
|---|---|
| Java | Application development |
| Maven | Dependency management and builds |
| Javalin | REST API development |
| Jackson | JSON serialization and deserialization |
| OpenCSV | CSV data processing |
| Java HTTP Client | Communication between REST services |
| ActiveMQ Classic | Asynchronous message broker |
| JMS | Publishing and consuming messages |
| Docker Compose | Running the ActiveMQ broker |

## Project Structure

```text
LogisticsConnect/
├── README.md
├── .gitignore
├── ingestion-service/
│   ├── README.md
│   ├── pom.xml
│   └── src/
├── hub-service/
│   ├── README.md
│   ├── pom.xml
│   └── src/
├── delay-stage-service/
│   ├── README.md
│   ├── pom.xml
│   └── src/
├── transit-service/
│   ├── README.md
│   ├── pom.xml
│   └── src/
├── alertbot/
│   ├── README.md
│   ├── pom.xml
│   └── src/
└── common/
    ├── README.md
    └── docker-compose.yml
```

## Prerequisites

Ensure the following tools are installed:

- Java 17 or later
- Maven 3.8 or later
- Docker with Docker Compose
- curl for testing REST endpoints

The project was developed using Java 21.

## Building the Project

Each service must be built separately because the repository does not contain a parent Maven configuration.

From the project root, build each module using Bash:

```bash
for service in ingestion-service hub-service delay-stage-service transit-service alertbot; do
    (
        cd "$service" &&
        mvn clean package
    ) || exit 1
done
```

On Windows PowerShell, build each module with:

```powershell
$services = @(
    "ingestion-service",
    "hub-service",
    "delay-stage-service",
    "transit-service",
    "alertbot"
)

foreach ($service in $services) {
    Push-Location $service
    mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        throw "Build failed: $service"
    }
    Pop-Location
}
```

Alternatively, enter each service directory and run:

```bash
mvn clean package
```

## Running LogisticsConnect

Each service runs as a separate Java process and requires its own terminal.

### Step 1: Start ActiveMQ

From the project root, on either Linux or Windows:

```bash
docker compose -f common/docker-compose.yml up -d
```

Verify the broker is running:

```bash
docker compose -f common/docker-compose.yml ps
```

### Step 2: Start the Services

Open a separate terminal for each service and run the corresponding commands from the project root. These commands work in Bash and PowerShell.

**Terminal 1 — Ingestion Service**

```bash
cd ingestion-service
java -jar target/ingestion-service.jar
```

**Terminal 2 — Hub Service**

```bash
cd hub-service
java -jar target/hub-service.jar
```

**Terminal 3 — Delay Stage Service**

```bash
cd delay-stage-service
java -jar target/delay-stage-service.jar
```

**Terminal 4 — Transit Service**

```bash
cd transit-service
java -jar target/transit-service.jar
```

**Terminal 5 — AlertBot**

```bash
cd alertbot
java -jar target/alertbot.jar
```

Start the broker and subscribers before publishing delay updates so that they can receive the events.

## REST API Overview

| Service | Method | Endpoint | Purpose |
|---|---|---|---|
| Ingestion | GET | `/health` | Health check |
| Ingestion | GET | `/hubs` | Retrieve cleaned hub records |
| Hub | GET | `/health` | Health check |
| Hub | GET | `/hubs/{hubId}` | Retrieve a hub |
| Delay Stage | GET | `/health` | Health check |
| Delay Stage | GET | `/delay-stage/{hubId}` | Retrieve a delay stage |
| Delay Stage | POST | `/delay-stage/{hubId}` | Update a delay stage |
| Transit | GET | `/health` | Health check |
| Transit | GET | `/hubs/{hubId}` | Retrieve transit estimates |
| AlertBot | GET | `/health` | Health check |

For detailed endpoint descriptions, refer to each service's README.

## End-to-End Testing

The following examples demonstrate the complete integration between the services using `curl`.

All services and the ActiveMQ broker must be running before testing.

### 1. Verify Service Health

**Linux / Git Bash**

```bash
curl http://localhost:7050/health
curl http://localhost:7051/health
curl http://localhost:7052/health
curl http://localhost:7053/health
curl http://localhost:7054/health
```

**Windows PowerShell**

```powershell
curl.exe http://localhost:7050/health
curl.exe http://localhost:7051/health
curl.exe http://localhost:7052/health
curl.exe http://localhost:7053/health
curl.exe http://localhost:7054/health
```

Expected response from each service:

```text
OK
```

### 2. Verify Data Ingestion

**Linux / Git Bash**

```bash
curl http://localhost:7050/hubs
```

**Windows PowerShell**

```powershell
curl.exe http://localhost:7050/hubs
```

Expected result: A JSON response containing cleaned hub records.

### 3. Retrieve Hub Information

**Linux / Git Bash**

```bash
curl http://localhost:7051/hubs/H-501
```

**Windows PowerShell**

```powershell
curl.exe http://localhost:7051/hubs/H-501
```

Expected result: The matching hub's information.

### 4. Set the Initial Delay Stage

Set the delay stage to `0` to establish a baseline.

**Linux / Git Bash**

```bash
curl -X POST http://localhost:7052/delay-stage/H-501 \
  -H "Content-Type: application/json" \
  -d '{"stage":0}'
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:7052/delay-stage/H-501" -ContentType "application/json" -Body '{"stage":0}'
```

### 5. Check the Initial Transit Estimate

**Linux / Git Bash**

```bash
curl http://localhost:7053/hubs/H-501
```

**Windows PowerShell**

```powershell
curl.exe http://localhost:7053/hubs/H-501
```

For delay stage `0`, the verified example is:

```json
{
  "hubId": "H-501",
  "delayStage": 0,
  "earliestArrivalMinutes": 50,
  "latestArrivalMinutes": 70
}
```

### 6. Update the Delay Stage

**Linux / Git Bash**

```bash
curl -X POST http://localhost:7052/delay-stage/H-501 \
  -H "Content-Type: application/json" \
  -d '{"stage":3}'
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:7052/delay-stage/H-501" -ContentType "application/json" -Body '{"stage":3}'
```

The Delay Stage Service updates the stage and publishes an event to ActiveMQ.

### 7. Verify the Updated Arrival Estimate

**Linux / Git Bash**

```bash
curl http://localhost:7053/hubs/H-501
```

**Windows PowerShell**

```powershell
curl.exe http://localhost:7053/hubs/H-501
```

Expected response:

```json
{
  "hubId": "H-501",
  "delayStage": 3,
  "earliestArrivalMinutes": 95,
  "latestArrivalMinutes": 115
}
```

This demonstrates how delay-stage updates affect the estimated arrival window.

### 8. Trigger an Alert

Update the delay stage to `5`.

**Linux / Git Bash**

```bash
curl -X POST http://localhost:7052/delay-stage/H-501 \
  -H "Content-Type: application/json" \
  -d '{"stage":5}'
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:7052/delay-stage/H-501" -ContentType "application/json" -Body '{"stage":5}'
```

Check the AlertBot terminal for:

```text
ALERT: Hub H-501 has severe delays. Stage: 5
```

This verifies the message flow from Delay Stage Service through ActiveMQ to AlertBot.

## Design Decisions

### Independent Services

Each service has a specific responsibility and can be built and run independently.

This separates data cleaning, hub retrieval, delay tracking, transit calculations, and alerting.

### REST for Synchronous Operations

REST is used when services require an immediate response, such as retrieving hub information.

### ActiveMQ for Asynchronous Events

ActiveMQ enables multiple subscribers to process delay-stage updates without the publisher directly calling each service.

This reduces direct coupling between the Delay Stage Service, Transit Service, and AlertBot.

### Centralized Data Cleaning

The Ingestion Service cleans the CSV data once and exposes standardized records through an API.

Downstream services consume the cleaned information instead of independently processing the original dataset.

### Simulated Alerting

AlertBot demonstrates event-driven notifications using console output rather than an external social media integration.

## Limitations

- Hub data originates from a local CSV file rather than a database.
- Delay-stage information is stored in memory and does not survive application restarts.
- Arrival estimates use predefined calculation rules rather than live traffic or weather information.
- AlertBot generates console notifications rather than sending real social media messages.
- The current messaging implementation does not provide durable subscriptions or guaranteed recovery of missed events.

## Project Status

| Stage | Implementation | Status |
|---|---|---|
| Stage 1 | CSV ingestion and cleaning | Completed |
| Stage 2 | REST service integration | Completed |
| Stage 3 | ActiveMQ publishing and subscription | Completed |
| Stage 4 | AlertBot simulated notifications | Completed |

All four stages were manually verified during development.

## Additional Documentation

Detailed documentation is available in the individual service directories:

- [Ingestion Service](ingestion-service/README.md)
- [Hub Service](hub-service/README.md)
- [Delay Stage Service](delay-stage-service/README.md)
- [Transit Service](transit-service/README.md)
- [AlertBot](alertbot/README.md)
- [ActiveMQ Configuration](common/README.md)

## Repository Verification

Verification code: `WTC-493LJ6QR`