# AI-Powered Industrial Incident & SLA Management Platform

## 1. Project Overview
(Worker Incident Diagnostic Assistant) is an enterprise-grade backend system designed for industrial factory environments. It leverages Local LLMs (**Llama2 via Ollama**) to automate the triage, classification, and routing of safety incidents reported by frontline workers. The system ensures operational safety and accountability through a strict **SLA Engine** and **Role-Based Access Control (RBAC)**.

---

## 2. System Architecture
The platform follows a **Clean Architecture (Hexagonal)** pattern, ensuring that core business logic remains independent of external frameworks and delivery mechanisms.

### Architectural Layers:
*   **Adapters (Outer Layer)**: Contains REST Controllers, DTOs, and infrastructure clients (e.g., Ollama API client).
*   **Application (Use Case Layer)**: Orchestrates business flows such as incident reporting, SLA monitoring, and assignment.
*   **Domain (Inner Core)**: Contains pure business entities (`Ticket`, `User`, `SlaPolicy`) and repository interfaces.
*   **Infrastructure/Configuration**: Handles cross-cutting concerns like Security (JWT), WebSocket broadcasting, and Persistence (PostgreSQL).

---

## 3. Technology Stack
*   **Framework**: Spring Boot 3.x
*   **Security**: Spring Security with JWT (Stateless)
*   **Persistence**: Spring Data JPA / PostgreSQL
*   **AI Engine**: Llama2 (via Ollama REST API)
*   **Real-time Communication**: Spring WebSocket / STOMP
*   **Documentation**: OpenAPI 3.0 (Swagger)
*   **Tools**: Lombok, MapStruct (Mappers), Jackson (JSON)

---

## 4. Backend Module Breakdown
*   **Auth Module**: Handles registration, login (JWT), and account locking mechanisms.
*   **Ticket Module**: Manages the lifecycle of an incident from creation to closure.
*   **AI Engine**: Specifically `IncidentClassificationService` for transforming natural language into structured data.
*   **SLA Engine**: Calculates deadlines based on severity and triggers escalations.
*   **Notification Module**: Real-time event publishing via WebSockets to role-specific topics.
*   **Chat Module**: Diagnostic AI assistant for workers to assess issues before reporting.

---

## 5. Role-Based Access Control (RBAC)
The system implements a hierarchical security model:
**ADMIN > MANAGER > SUPERVISOR > WORKER**

| Role | Permissions |
| :--- | :--- |
| **Worker** | Report incidents, track personalized dashboard, AI diagnostic chat. |
| **Supervisor** | Triage incidents for their department, assign workers, add internal comments. |
| **Manager** | View enterprise KPIs, oversee resolution, handle escalated SLA breaches. |
| **Admin** | Full system visibility, user management, system health monitoring. |

---

## 6. Incident Lifecycle
1.  **Creation**: Worker submits a report (Manual or via AI).
2.  **Triage**: AI classifies type, severity, and routes to the correct Department.
3.  **Assignment**: Supervisor assigns a Worker/Technician to fix the issue.
4.  **In Progress**: Status update as the issue is being handled.
5.  **Resolution**: Technician marks as resolved.
6.  **Closure**: Manager or Reporter closes the ticket after verification.

---

## 7. AI Classification Pipeline
When a worker reports an incident, the message undergoes the following processing:

```text
Worker Message: "The cooling pump on Machine B2 is leaking oil and overheating."
      ↓
Structure Prompt Injection (System: You are a factory classifier...)
      ↓
LLM Inference (Llama2 @ Local Ollama)
      ↓
JSON Output: {
  "incidentType": "MACHINE_FAILURE",
  "severity": "HIGH",
  "department": "MAINT",
  "priority": "P2",
  "suggestedActions": ["Shut down power", "Check oil seal"]
}
      ↓
Ticket Entity Generation (Persisted to Database)
```

---

## 8. SLA Engine Logic
SLA deadlines are calculated automatically upon ticket creation based on severity levels defined in the `SlaEngineService` or `SlaPolicy` entity.

### Default SLA Thresholds:
| Severity | Resolution Target | Escalation Trigger |
| :--- | :--- | :--- |
| **CRITICAL** | 30 Minutes | 10 Minutes |
| **HIGH** | 4 Hours | 80 Minutes |
| **MEDIUM** | 24 Hours | 8 Hours |
| **LOW** | 72 Hours | 24 Hours |

*Calculation: `Deadline = CreationTime + TargetResolutionMinutes`*

---

---

## 9. Secure Configuration & Setup

### Secret Management
To run this project safely without exposing credentials:
1.  **Environment Variables**: All secrets are externalized. Set the following variables in your environment:
    - `DB_PASSWORD`: Your PostgreSQL password.
    - `JWT_SECRET`: A secure 256-bit string for signing tokens.
2.  **Local Overrides**: You can also use `src/main/resources/application-local.yml` for local development. This file is ignored by Git.

### Prerequisites
- **Java 21+**
- **Maven 3.8+**
- **Ollama** running locally at `http://localhost:11434`
- **PostgreSQL** running at `localhost:5432`

### Setup Steps
1.  **Configure Environment**: Set `DB_PASSWORD` and `JWT_SECRET`.
2.  **Start Ollama**: `ollama serve` & `ollama pull llama2`
3.  **Build & Run**:
    ```bash
    ./mvnw clean package
    java -jar target/localchat-backend-1.0.0-SNAPSHOT.jar
    ```
4.  **Swagger UI**: `http://localhost:8081/swagger-ui.html`

---

## 10. API Endpoints (Core)
| Method | Endpoint | Description | Role |
| :--- | :--- | :--- | :--- |
| POST | `/api/v1/auth/login` | Authenticate & get JWT | Public |
| POST | `/api/v1/worker/incidents/report` | Report new incident | Worker |
| GET | `/api/v1/supervisor/incidents` | List dept incidents | Supervisor |
| POST | `/api/v1/chat` | AI diagnostic help | Authenticated |
| PUT | `/api/v1/manager/tickets/{id}/assign` | Assign technician | Manager |
| GET | `/api/v1/admin/system/health` | Check system status | Admin |

---

## 11. Security Model
*   **Stateless Authentication**: Every request requires a `Bearer <JWT>` in the Authorization header.
*   **Departmental Scoping**: Supervisors and Managers are limited to their assigned department.
*   **Account Protection**: Automatic account locking after 5 failed login attempts.

---

## 12. Future Improvements
*   **Multimodal AI**: Image-based failure analysis.
*   **Predictive Maintenance**: Historical trend analysis for proactive alerts.
*   **Edge Deployment**: Localized AI nodes for low-latency floor operations.

---
*Last Updated: 2026-03-11*
*Built with ❤️ for industrial AI exploration*
