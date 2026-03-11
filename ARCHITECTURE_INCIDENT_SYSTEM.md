# AI-Driven Factory Incident & SLA Management Platform
## System Architecture & Design Document

---

## 🎯 Executive Summary

A production-grade incident management system that converts unstructured factory worker messages into structured tickets with:
- **AI Classification**: Natural language → structured incident data
- **SLA Management**: Automatic deadline calculation
- **Department Routing**: Intelligent assignment based on incident type
- **Action Planning**: AI-suggested mitigation actions
- **Real-time Dashboard**: Incident tracking and analytics

---

## 📋 System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                            │
│         ┌───────────────────────────────────────────┐            │
│         │ Swagger UI / REST API                     │            │
│         │ POST /api/v1/incidents/report             │            │
│         │ GET /api/v1/incidents/{id}                │            │
│         │ GET /api/v1/incidents?status=OPEN         │            │
│         └───────────────────────────────────────────┘            │
└──────────────┬────────────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────────────┐
│              CONTROLLER LAYER                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ IncidentReportController                                 │   │
│  │ - receives worker message                                │   │
│  │ - validates input                                        │   │
│  │ - delegates to service layer                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────┬────────────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────────────┐
│              SERVICE LAYER (Business Logic)                       │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ TicketService                                            │   │
│  │ - orchestrates ticket creation workflow                  │   │
│  │ - coordinates between AI, SLA, and repository layers     │   │
│  │ - applies business rules                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────┬────────────────────────────────────────────────────┘
               │
         ┌─────┴─────┬──────────────┬──────────────┐
         │            │              │              │
    ┌────▼──┐  ┌─────▼──┐  ┌──────▼─┐  ┌────────▼─┐
    │  AI   │  │  SLA   │  │ Dept   │  │ Incident │
    │Engine │  │Engine  │  │Router  │  │ History  │
    └───────┘  └────────┘  └────────┘  └──────────┘
         │            │              │              │
         └─────┬──────┴──────────────┴──────────────┘
               │
┌──────────────▼────────────────────────────────────────────────────┐
│              AI / ENGINE LAYER                                    │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ IncidentClassificationService                            │   │
│  │ - sends message to Llama2 via Ollama                    │   │
│  │ - parses structured JSON response                        │   │
│  │ - returns classification DTO                             │   │
│  │                                                          │   │
│  │ SlaEngineService                                         │   │
│  │ - calculates SLA based on severity/priority             │   │
│  │ - returns target resolution deadline                     │   │
│  │                                                          │   │
│  │ DepartmentRouterService                                  │   │
│  │ - determines responsible department                      │   │
│  │ - assigns to appropriate team                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────┬────────────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────────────┐
│              REPOSITORY LAYER (Data Access)                       │
│                                                                    │
│  ├── TicketRepository                                             │
│  ├── IncidentTypeRepository                                       │
│  ├── DepartmentRepository                                         │
│  ├── SlaRepository                                                │
│  └── IncidentHistoryRepository                                    │
└──────────────┬────────────────────────────────────────────────────┘
               │
┌──────────────▼────────────────────────────────────────────────────┐
│              PERSISTENCE LAYER                                    │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           PostgreSQL Database                            │   │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────┐      │   │
│  │  │  tickets   │  │ incidents  │  │ sla_policies │      │   │
│  │  └────────────┘  └────────────┘  └──────────────┘      │   │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────┐      │   │
│  │  │   users    │  │departments │  │incident_types│      │   │
│  │  └────────────┘  └────────────┘  └──────────────┘      │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              EXTERNAL INTEGRATIONS                               │
│                                                                  │
│  ┌──────────────────────┐      ┌──────────────────────┐        │
│  │   Ollama Server      │      │  Llama2 Model        │        │
│  │ (http://localhost:11434)   │  (language model)     │        │
│  └──────────────────────┘      └──────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Component Responsibilities

### **1. Controller Layer**
**Responsibility**: HTTP request handling & response formatting

`IncidentReportController`:
- Receives worker incident messages
- Validates input format and constraints
- Delegates to TicketService
- Formats and returns API responses
- Handles HTTP status codes

---

### **2. Service Layer**
**Responsibility**: Business logic orchestration

`TicketService`:
```
Worker Message
    ↓
[Classify via AI] → Extract incident details
    ↓
[Calculate SLA] → Determine deadline
    ↓
[Route Department] → Assign responsibility
    ↓
[Generate Ticket ID] → INC-1001, INC-1002, ...
    ↓
[Persist to DB] → Save in PostgreSQL
    ↓
[Return Response] → TicketCreatedResponse DTO
```

---

### **3. AI Engine Layer**
**Responsibility**: Artificial intelligence & LLM integration

#### **IncidentClassificationService**
- Sends worker message to Llama2
- Uses structured prompting for JSON responses
- Parses JSON into Java DTOs
- Returns: `IncidentClassificationDto`
  - `incidentType`: MACHINE_FAILURE, SAFETY_HAZARD, QUALITY_ISSUE, etc.
  - `severity`: CRITICAL, HIGH, MEDIUM, LOW
  - `department`: Production / Maintenance / Quality / Safety
  - `priority`: P1, P2, P3, P4
  - `suggestedActions`: List of recommended mitigations

Example prompt sent to Llama2:
```
You are a factory incident classifier.
Analyze worker message and respond ONLY with JSON.

Worker Message: "Machine stopped due to overheating"

Respond with JSON (no markdown, no extra text):
{
  "incidentType": "MACHINE_FAILURE",
  "severity": "HIGH",
  "department": "MAINTENANCE",
  "priority": "P2",
  "suggestedActions": [
    "Check coolant levels",
    "Inspect thermal sensors",
    "Review recent maintenance logs"
  ]
}
```

#### **SlaEngineService**
- Maps severity/priority to SLA minutes
- Calculates resolution deadline (created_time + slaMinutes)
- Rules:
  - CRITICAL → 30 minutes
  - HIGH → 240 minutes (4 hours)
  - MEDIUM → 1440 minutes (24 hours)
  - LOW → 4320 minutes (72 hours)

#### **DepartmentRouterService**
- Assigns incident to appropriate department
- Uses incident type + classification
- Returns: `Department` entity with contact info

---

### **4. Repository Layer**
**Responsibility**: Database abstraction via JPA

- `TicketRepository`: CRUD for tickets
- `IncidentTypeRepository`: Predefined incident types
- `DepartmentRepository`: Organizational departments
- `SlaRepository`: SLA policy management
- `IncidentHistoryRepository`: Audit trail & analytics

---

### **5. Entity Layer**
**Responsibility**: Domain models mapped to database

```
Ticket
├── ticketId (PK): INC-1001
├── workerId (FK): worker-23
├── message: "Machine stopped..."
├── incidentType (FK): MACHINE_FAILURE
├── severity: HIGH
├── department (FK): MAINTENANCE
├── priority: P2
├── slaTargetMinutes: 240
├── slaDeadline: 2026-03-06T12:45:00Z
├── status: OPEN (OPEN, IN_PROGRESS, RESOLVED, CLOSED)
├── suggestedActions: ["Action 1", "Action 2"]
├── createdAt: 2026-03-06T12:15:00Z
├── resolvedAt: null
└── notes: [Ticket Comments]
```

---

## 📊 Request Flow Diagram

```
1. WORKER MESSAGE
   ──────────────
   POST /api/v1/incidents/report
   {
     "sessionId": "worker-23",
     "message": "Machine stopped due to overheating"
   }


2. CONTROLLER VALIDATION
   ─────────────────────
   IncidentReportController
   ├─ Validate sessionId not blank
   ├─ Validate message not blank
   └─ Validate message length


3. AI CLASSIFICATION
   ──────────────────
   IncidentClassificationService
   ├─ Build prompt with message
   ├─ Call Ollama/Llama2
   ├─ Parse JSON response
   └─ Return: IncidentClassificationDto


4. SLA CALCULATION
   ────────────────
   SlaEngineService
   ├─ Map severity → minutes (HIGH → 240)
   ├─ Calculate deadline = now + 240 min
   └─ Return: SLA deadline


5. DEPARTMENT ROUTING
   ──────────────────
   DepartmentRouterService
   ├─ Lookup department by type (MACHINE_FAILURE → MAINTENANCE)
   └─ Return: Department entity


6. TICKET GENERATION
   ──────────────────
   TicketService
   ├─ Generate ID format: INC-{XXXX}
   ├─ Create Ticket entity
   ├─ Map all fields
   └─ Set initial status: OPEN


7. PERSISTENCE
   ────────────
   TicketRepository.save(ticket)
   └─ INSERT into tickets table


8. RESPONSE
   ────────
   Return: IncidentCreatedResponse
   {
     "ticketId": "INC-1042",
     "incidentType": "MACHINE_FAILURE",
     "severity": "HIGH",
     "department": "MAINTENANCE",
     "priority": "P2",
     "slaTargetMinutes": 240,
     "slaDeadline": "2026-03-06T12:55:00Z",
     "status": "OPEN",
     "suggestedActions": [...]
   }
```

---

## 🏢 Module Dependencies

```
                    TicketService
                    /     │      \
                   /      │       \
              ClassifyService  SlaEngine  DeptRouter
                   │             │           │
                   │             │           │
            IncidentClassificationService   DepartmentRepository
                   │
                   └─→ OllamaRestClient (calls Llama2)
```

---

## 🔐 Data Flow & Separation of Concerns

```
Presentation ────┐
(Controller)     │ (HTTP handling)
                 ↓
Service ────┐────────────────┬──────────────┐
(Business)  │ (Orchestration)│ (Logic)      │
            ↓                ↓              ↓
AI Engine ──────────────┐
(LLM)      │            │ (AI Classification)
           |            └──→ Parse JSON
           |                 Create DTO
Persistence ───────────────────────────────────┐
(DB)       │ (JPA/SQL)                          │
           ↓                                    ↓
         Entities ←────────────── Repositories
```

---

## 📈 Scalability Considerations

1. **Async Processing**: Use Spring @Async or RabbitMQ for AI classification
2. **Caching**: Cache classification results for similar messages
3. **Load Balancing**: Multiple Ollama instances with round-robin
4. **Database**: PostgreSQL with read replicas for reporting
5. **Monitoring**: Spring Boot Actuator + Prometheus metrics
6. **Circuit Breaker**: Resilience4j for Ollama failures

---

## ✅ Success Criteria

- Worker can send incident message via API
- AI classifies and structures the incident
- Ticket created with correct SLA
- Department assigned automatically
- Response received within 5 seconds
- All data persisted in PostgreSQL
- Swagger documentation complete
- 99.5% uptime
- < 2% error rate

