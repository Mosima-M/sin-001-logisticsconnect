# Ingestion Service

## Overview

The Ingestion Service is responsible for reading, cleaning, and exposing hub data from the legacy `hubs-global.csv` dataset.

It is the first service in the LogisticsConnect system and provides standardized hub information to the Hub Service through a REST API.

The service runs independently as a Maven project on port `7050`.

## Responsibilities

The service performs the following operations:

- Reads hub records from `hubs-global.csv` using OpenCSV.
- Removes leading and trailing whitespace.
- Standardizes hub identifiers to uppercase.
- Normalizes province names to a consistent format.
- Standardizes boolean values representing active and inactive hubs.
- Handles missing or unrecognized values.
- Detects duplicate hub identifiers and excludes repeated records.
- Exposes the cleaned dataset through a REST endpoint.

## Data Cleaning

The CSV dataset contains four columns:

| Column | Description |
|---|---|
| `hub_id` | Unique identifier for a hub |
| `Province` | Province where the hub is located |
| `sorting_center` | Name of the sorting center |
| `active` | Indicates whether the hub is active |

### Cleaning rules

| Field | Cleaning approach |
|---|---|
| Hub ID | Trim whitespace and convert to uppercase |
| Province | Normalize casing and known formatting variations |
| Sorting center | Trim surrounding whitespace |
| Active | Convert recognized true/false representations into consistent values |
| Duplicate IDs | Retain the first occurrence of a normalized hub ID |
| Missing or invalid flags | Represent unrecognized values as `unknown` |

### Example

**Raw CSV data:**

```csv
hub_id,Province,sorting_center,active
h-502 ,gauteng,Pretoria North,0
H-505,Western Cape,Cape Town Port,TRUE
```

**Cleaned representation:**

```json
[
  {
    "hubId": "H-502",
    "province": "Gauteng",
    "sortingCenter": "Pretoria North",
    "active": "false"
  },
  {
    "hubId": "H-505",
    "province": "Western Cape",
    "sortingCenter": "Cape Town Port",
    "active": "true"
  }
]
```

## REST API

**Base URL:** `http://localhost:7050`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Returns `OK` when the service is running |
| GET | `/hubs` | Returns the cleaned hub records as JSON |

### Example request

```http
GET http://localhost:7050/hubs
```

The endpoint provides cleaned records that other services can consume without having to process the original CSV themselves.

## Technologies

- Java
- Maven
- Javalin
- OpenCSV
- Jackson (JSON serialization)

## Build and Run

From the `ingestion-service` directory:

**Build:**

```bash
mvn clean package
```

**Run:**

```bash
java -jar target/ingestion-service.jar
```

The service starts on port `7050`.

## Testing

The endpoints can be verified using PowerShell:

**Health check:**

```powershell
Invoke-RestMethod http://localhost:7050/health
```

Expected response:

```text
OK
```

**Retrieve cleaned hubs:**

```powershell
Invoke-RestMethod http://localhost:7050/hubs
```

The response should contain the normalized hub records.

Run any automated tests configured for this module with:

```bash
mvn test
```

## Design Decisions

### Centralized data cleaning

Data cleaning is performed in the Ingestion Service rather than repeated across downstream services.

This ensures that the Hub Service receives consistently formatted data.

### Duplicate handling

Normalized hub IDs are used to detect duplicates. The first occurrence is retained, preventing multiple records with the same identifier from appearing in the cleaned output.

### In-memory processing

The dataset is loaded and processed without introducing a separate database.

This keeps the service lightweight and appropriate for the project's scope.

## Limitations

- Data is sourced from a local CSV file rather than a database.
- Duplicate detection based on hub identifiers does not detect every possible real-world duplicate.
- Unknown values are preserved as normalized placeholders rather than inferred.
- The service does not provide persistent data updates through its API.

For the complete system architecture and instructions for running all services, see the [main LogisticsConnect README](../README.md).