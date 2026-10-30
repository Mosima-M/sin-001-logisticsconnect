# Hub Service

## Overview

The Hub Service is responsible for retrieving and serving hub information within the LogisticsConnect system.

It communicates with the Ingestion Service through HTTP to access cleaned hub records, rather than reading and processing the CSV file itself.

The service is an independent Maven application running on port `7051`.

## Responsibilities

The Hub Service:

- Retrieves cleaned hub records from the Ingestion Service.
- Searches for a hub using its unique identifier.
- Returns hub details as JSON.
- Handles requests for hubs that do not exist.
- Provides hub information to downstream services, including the Transit Service.

## How It Works

The Hub Service uses synchronous REST communication.

1. A client requests a hub using its identifier.
2. The Hub Service sends an HTTP request to the Ingestion Service.
3. The Ingestion Service returns the cleaned hub records.
4. The Hub Service searches for the requested hub.
5. The matching record is returned as JSON.

### Communication Flow

```text
Client / Transit Service
          |
          | GET /hubs/H-501
          v
    Hub Service
      Port 7051
          |
          | GET /hubs
          v
  Ingestion Service
      Port 7050
          |
          | Cleaned hub records
          v
    Hub Service
          |
          | Matching hub as JSON
          v
        Client
```

## REST API

**Base URL:** `http://localhost:7051`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Returns `OK` when the service is running |
| GET | `/hubs/{hubId}` | Retrieves a hub by its identifier |

### Example Request

```http
GET http://localhost:7051/hubs/H-501
```

A successful request returns the matching hub's information, including its identifier, province, sorting center, and active status.

### Error Handling

| Status Code | Meaning |
|---|---|
| `200 OK` | Hub retrieved successfully |
| `404 Not Found` | Requested hub does not exist |
| `502 Bad Gateway` | The Ingestion Service is unavailable or cannot provide a valid response |

## Technologies

- Java
- Maven
- Javalin
- Java HTTP Client
- Jackson for JSON processing

## Build and Run

The Ingestion Service must be running on port `7050` for hub lookups to work.

From the `hub-service` directory:

**Build:**

```bash
mvn clean package
```

**Run:**

```bash
java -jar target/hub-service.jar
```

The Hub Service starts on port `7051`.

## Testing

### Health Check

```powershell
Invoke-RestMethod http://localhost:7051/health
```

Expected response:

```text
OK
```

### Retrieve a Hub

```powershell
Invoke-RestMethod http://localhost:7051/hubs/H-501
```

Expected result: The matching hub record is returned as JSON.

### Retrieve an Unknown Hub

```powershell
Invoke-RestMethod http://localhost:7051/hubs/H-999
```

Expected result: An HTTP `404 Not Found` response, assuming the identifier is not present in the dataset.

### Automated Tests

Run any configured tests using:

```bash
mvn test
```

## Design Decisions

### Separation of Responsibilities

The Ingestion Service handles CSV reading and cleaning, while the Hub Service handles hub lookup requests.

This separation avoids duplicating data-cleaning logic across services.

### Synchronous REST Communication

REST is used because the Hub Service needs an immediate response containing the requested hub information.

This differs from the asynchronous messaging used for delay-stage updates elsewhere in LogisticsConnect.

### Reusing Cleaned Data

By consuming the Ingestion Service's API, the Hub Service works with standardized records instead of the original inconsistent CSV data.

## Limitations

- Hub lookups depend on the availability of the Ingestion Service.
- Hub records are retrieved from an upstream service rather than stored in a dedicated database.
- The service provides read-only access to hub information.

For the complete system architecture, see the [main LogisticsConnect README](../README.md).