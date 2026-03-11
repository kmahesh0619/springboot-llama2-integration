# WIDA: Factory Incident Management System - Functional Documentation

## 1. Project Overview
**WIDA** (Wireless Industrial Diagnostic Assistant) is a comprehensive industrial incident management platform designed for factory floor operations. It integrates AI-driven diagnostics, real-time SLA tracking, and role-based workflows to streamline equipment maintenance and safety reporting.

The system consists of a **Spring Boot Backend** providing a robust REST API and WebSocket events, and a **Kotlin Multiplatform (KMP) Client** supporting Android and Desktop platforms with a shared UI built in Compose Multiplatform.

---

## 2. System Architecture

### 2.1 Backend Strategy (Spring Boot)
- **Hexagonal Architecture**: Separation of adapters (REST, WebSockets), application logic (Services), and domain entities.
- **AI Integration**: Leverages **Ollama (Llama-2)** for unstructured incident message classification.
- **SLA Engine**: Automated calculation of resolution deadlines based on severity and priority.
- **Real-time Engine**: Spring WebSocket + STOMP for live ticket updates.

### 2.2 Frontend Strategy (KMP / Compose)
- **Shared UI**: Single codebase for UI logic in `commonMain` using Compose Multiplatform.
- **Navigation**: Managed by **Voyager** with role-aware screen routing.
- **Dependency Injection**: Unified setup using **Koin**.
- **State Management**: Unidirectional Data Flow (UDF) using `StateFlow` and ViewModels (`ScreenModel`).
- **Networking**: **Ktor Client** with centralized authentication and response handling.

---

## 3. Authentication & Session Management

### 3.1 Security Model
- **JWT Authentication**: Secure stateless authentication for all API interactions.
- **Role Hierarchy**: `ADMIN > MANAGER > SUPERVISOR > WORKER`.
- **Account Protection**: Automated account locking after consecutive failed login attempts.

### 3.2 Session Lifecycle
- **Persistence**: Tokens and user metadata are stored securely using `Multiplatform Settings`.
- **Session Restoration**: The application automatically restores the last valid session on startup, routing the user directly to their role-specific dashboard.
- **Interceptor Layer**: A Ktor interceptor automatically injects the Bearer token into the `Authorization` header of every outgoing request.

---

## 4. Role-Based Feature Matrix

### 🛠 WORKER (Foundational Role)
*Focus: Reporting, basic tracking, and diagnostic support.*
- **Incident Reporting**:
  - **Manual Flow**: Form-based entry for structured reporting.
  - **AI Chat Flow**: Natural language reporting via interactive chat.
- **My Dashboard**: View a personalized list of submitted incidents.
- **Ticket Operations**:
  - **Withdraw**: Cancel an "OPEN" ticket if no longer relevant.
  - **Reopen**: Revive a "RESOLVED" ticket with a new comment.
- **AI Diagnostic Chat**: Get immediate troubleshooting advice from Llama-2.

### 📋 SUPERVISOR (Operational Role)
*Focus: Triage, assignment, and status management.*
- **Global Triage**: Access a system-wide view of all `OPEN` tickets.
- **Incident Management**:
  - **Assign**: Direct assignment of tickets to specific workers or self-assignment.
  - **Status Updates**: Move tickets through `IN_PROGRESS`, `RESOLVED`, or `ESCALATED` states.
  - **Commenting**: Append administrative notes to the incident history.
- **Audit View**: Access the full chronological history of ticket events.

### 📊 MANAGER (Strategic Role)
*Focus: Performance monitoring, escalation, and final resolution.*
- **Live KPI Dashboard**: Real-time monitoring of:
  - **SLA Breaches**: Immediate visibility into overdue tasks.
  - **Status Counts**: Breakdown of Open, Active, and Resolved tickets.
- **Ticket Authority**:
  - **Closure**: Final review and `CLOSE` action on resolved tickets.
  - **Strategic Assignment**: Routing tickets to appropriate supervisors.
- **Escalation Support**: Handling and resolving tickets marked for higher-level attention.

### 🛡 ADMIN (Universal Role)
*Focus: Total system oversight and user administration.*
- **Omni-Dashboard**: Merged performance view across all departments and roles.
- **Global Control**: Unrestricted ability to Modify/Cancel/Resolve *any* ticket.
- **User Management**:
  - **CRUD Operations**: Search, view, and update any user.
  - **Account Control**: Role modification, department reassignment, and account deactivation.
- **System Health**: Monitor backend health, database status, and AI service availability.

---

## 5. API & Data Flow

### 5.1 Incident Lifecycle Flow
1. **Intake**: Worker sends message → AI (Llama-2) classifies Type, Severity, and Department.
2. **Routing**: Ticket is created → SLA deadline calculated → Department supervisor notified via WebSocket.
3. **Execution**: Supervisor assigns worker → Status moves to `IN_PROGRESS`.
4. **Resolution**: Work completed → Status moves to `RESOLVED`.
5. **Closure**: Manager verifies → Status moves to `CLOSED`.

### 5.2 Key API Endpoints
| Role | Endpoint | Method | Key Feature |
|------|----------|--------|-------------|
| **Auth** | `/api/v1/auth/login` | `POST` | Authenticate and get JWT |
| **Worker** | `/api/v1/worker/incidents/report` | `POST` | Create manual report |
| **Worker** | `/api/v1/chat/message` | `POST` | AI-driven reporting |
| **Supervisor** | `/api/v1/supervisor/incidents` | `GET` | List filtered incidents |
| **Supervisor** | `/api/v1/supervisor/incidents/{id}/assign` | `PUT` | Assign ticket owner |
| **Manager** | `/api/v1/manager/tickets/stats` | `GET` | Fetch KPI metrics |
| **Admin** | `/api/v1/admin/users` | `GET` | Manage system users |

---

## 6. Real-time Features (WebSockets)
The application utilizes a dedicated `SocketManager` to maintain a persistent connection for real-time updates:
- **Topics**: Role-based topics (e.g., `/topic/supervisor/{dept}`) ensure targeted delivery.
- **Reactive Refresh**: Incoming socket events trigger a background REST refresh in the ViewModels, keeping the UI current without manual reloading.
- **Resilience**: Implements exponential back-off for automatic reconnection during network instability.

---

## 7. Summary Table

| Feature | Worker | Supervisor | Manager | Admin |
|---------|:---:|:---:|:---:|:---:|
| Report Incident | ✅ | ✅ | ✅ | ✅ |
| AI Chat Diagnostics | ✅ | ✅ | ✅ | ✅ |
| View Own Tickets | ✅ | ✅ | ✅ | ✅ |
| Triage All Tickets | ❌ | ✅ | ✅ | ✅ |
| Assign Personnel | ❌ | ✅ | ✅ | ✅ |
| Resolve Tickets | ❌ | ✅ | ✅ | ✅ |
| Close Tickets | ❌ | ❌ | ✅ | ✅ |
| Dashboard Metrics | ❌ | ❌ | ✅ | ✅ |
| User Management | ❌ | ❌ | ❌ | ✅ |
| Force System Actions | ❌ | ❌ | ❌ | ✅ |

---
**Document Status**: Finalized
**Architecture Version**: 2.4 (March 2026)
