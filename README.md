# Smart Campus API

A RESTful API for managing rooms and environmental sensors across a university smart campus, built with Java 11 and Jersey (JAX-RS).

---

## Overview

### Architecture

The Smart Campus API follows a standard layered REST architecture:

```
Client
  │
  ▼
Jersey Servlet (JAX-RS)  ←  web.xml maps /api/v1/* to Jersey
  │
  ├── LoggingFilter        (request/response logging)
  ├── GlobalExceptionMapper (unified error responses)
  │
  ├── DiscoveryResource    GET /
  ├── RoomResource         /rooms
  ├── SensorResource       /sensors
  └── SensorReadingResource /sensors/{id}/readings
        │
        ▼
      DataStore            (ConcurrentHashMap — in-memory, thread-safe)
```


### Data Model

```
Room
 ├── id         (String, unique)
 ├── name       (String)
 ├── capacity   (int)
 └── sensorIds  (List<String>)

Sensor
 ├── id           (String, unique)
 ├── type         (String — e.g. "Temperature", "CO2")
 ├── status       (ACTIVE | MAINTENANCE | OFFLINE)
 ├── currentValue (double)
 └── roomId       (String, must reference an existing Room)

SensorReading
 ├── id        (UUID, auto-generated)
 ├── timestamp (long ms, auto-generated)
 └── value     (double)
```

---

## Build and Run Instructions

### Prerequisites

- Java 11 or later — verify with `java -version`
- Apache Maven 3.6+ — verify with `mvn -version`
- Apache Tomcat 9.x — [download here](https://tomcat.apache.org/download-90.cgi)

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd smart-campus
```

### 2. Build the WAR

```bash
mvn clean package
```

The build produces:

```
target/smart-campus-api.war
```

### 3. Deploy to Tomcat

**Option A — Copy WAR manually:**

```bash
cp target/smart-campus-api.war /path/to/tomcat/webapps/
```

Then start Tomcat:

```bash
# Linux / macOS
/path/to/tomcat/bin/startup.sh

# Windows
C:\tomcat\bin\startup.bat
```

**Option B — Use the Tomcat Manager (if enabled):**

Open `http://localhost:8080/manager/html`, scroll to "Deploy", and upload `smart-campus-api.war`.

### 4. Verify the server is running

```bash
curl http://localhost:8080/api/v1/
```

Expected response:

```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "description": "Sensor & Room Management API for University Smart Campus",
  "contact": "admin@smartcampus.ac.uk",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```



## curl Examples

All examples assume the API is running at `http://localhost:8080`.

### 1. List all rooms

```bash
curl -X GET http://localhost:8080/api/v1/rooms \
  -H "Accept: application/json"
```

**Expected response:**

```json
[
  {
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 50,
    "sensorIds": ["TEMP-001"]
  },
  {
    "id": "LAB-101",
    "name": "Computer Lab",
    "capacity": 30,
    "sensorIds": ["CO2-001"]
  }
]
```

---

### 2. Create a new room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "HALL-205",
    "name": "Lecture Hall B",
    "capacity": 120
  }'
```

**Expected response (201 Created):**

```json
{
  "id": "HALL-205",
  "name": "Lecture Hall B",
  "capacity": 120,
  "sensorIds": []
}
```

---

### 3. Create a sensor and assign it to a room

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-002",
    "type": "Temperature",
    "roomId": "HALL-205",
    "status": "ACTIVE",
    "currentValue": 21.0
  }'
```

**Expected response (201 Created):**

```json
{
  "id": "TEMP-002",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 21.0,
  "roomId": "HALL-205"
}
```

---

### 4. Record a sensor reading

```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{ "value": 23.7 }'
```

**Expected response (201 Created):**

```json
{
  "id": "a1b2c3d4-...",
  "timestamp": 1714000000000,
  "value": 23.7
}
```

> After this call, `TEMP-002`'s `currentValue` is updated to `23.7`.

---

### 5. Filter sensors by type

```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature" \
  -H "Accept: application/json"
```

**Expected response:**

```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  },
  {
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 23.7,
    "roomId": "HALL-205"
  }
]
```

---

### 6. Retrieve all readings for a sensor

```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Accept: application/json"
```

**Expected response:**

```json
[
  {
    "id": "a1b2c3d4-...",
    "timestamp": 1714000000000,
    "value": 23.7
  }
]
```

---

### 7. Delete a room (only works when no sensors are assigned)

First, create a room with no sensors:

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{ "id": "TEMP-ROOM", "name": "Temporary Room", "capacity": 10 }'
```

Then delete it:

```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/TEMP-ROOM
```

**Expected response: `204 No Content`**

Attempting to delete `LAB-101` (which has `CO2-001` assigned) returns:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room LAB-101 cannot be deleted because it still has sensors assigned."
}
```

---

## Project Structure

```
smart-campus/
├── pom.xml
└── src/main/
    ├── java/com/smartcampus/
    │   ├── application/
    │   │   ├── SmartCampusApplication.java   # Jersey app config
    │   │   └── DataStore.java                # In-memory singleton store
    │   ├── exception/
    │   │   ├── GlobalExceptionMapper.java
    │   │   ├── RoomNotEmptyException.java
    │   │   ├── LinkedResourceNotFoundException.java
    │   │   └── SensorUnavailableException.java
    │   ├── filter/
    │   │   └── LoggingFilter.java            # Request/response logging
    │   ├── model/
    │   │   ├── Room.java
    │   │   ├── Sensor.java
    │   │   └── SensorReading.java
    │   └── resource/
    │       ├── DiscoveryResource.java         # GET /
    │       ├── RoomResource.java              # /rooms
    │       ├── SensorResource.java            # /sensors
    │       └── SensorReadingResource.java     # /sensors/{id}/readings
    └── webapp/WEB-INF/
        └── web.xml                            # Servlet mapping
```
