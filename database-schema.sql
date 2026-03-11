-- ═══════════════════════════════════════════════════════════════════════════════
-- AI-DRIVEN FACTORY INCIDENT & SLA MANAGEMENT SYSTEM
-- PostgreSQL Database Schema
-- ═══════════════════════════════════════════════════════════════════════════════

-- Create database
CREATE DATABASE IF NOT EXISTS factory_incidents;

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 1: USERS (Factory Workers)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    worker_id VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    department_id BIGINT,
    email VARCHAR(255),
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL DEFAULT 'WORKER', -- WORKER, SUPERVISOR, MANAGER, ADMIN
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_worker_id ON users(worker_id);
CREATE INDEX idx_users_department_id ON users(department_id);
CREATE INDEX idx_users_email ON users(email);

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 2: DEPARTMENTS
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS departments (
    department_id BIGSERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    manager_user_id BIGINT,
    email VARCHAR(255),
    phone VARCHAR(20),
    slack_channel VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (manager_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_departments_code ON departments(department_code);

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 3: INCIDENT TYPES
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS incident_types (
    incident_type_id BIGSERIAL PRIMARY KEY,
    type_name VARCHAR(100) NOT NULL UNIQUE,
    type_code VARCHAR(50) NOT NULL UNIQUE, -- e.g., MACHINE_FAILURE, SAFETY_HAZARD
    description TEXT,
    primary_department_id BIGINT NOT NULL,
    default_severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- CRITICAL, HIGH, MEDIUM, LOW
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (primary_department_id) REFERENCES departments(department_id)
);

CREATE INDEX idx_incident_types_code ON incident_types(type_code);
CREATE INDEX idx_incident_types_department ON incident_types(primary_department_id);

-- Insert sample incident types
INSERT INTO incident_types (type_name, type_code, description, primary_department_id, default_severity)
VALUES 
    ('Machine Failure', 'MACHINE_FAILURE', 'Equipment malfunction or breakdown', 2, 'HIGH'),
    ('Safety Hazard', 'SAFETY_HAZARD', 'Worker safety risk or accident', 3, 'CRITICAL'),
    ('Quality Issue', 'QUALITY_ISSUE', 'Product defect or non-conformance', 4, 'MEDIUM'),
    ('Maintenance Request', 'MAINTENANCE_REQUEST', 'Preventive or corrective maintenance', 2, 'MEDIUM'),
    ('Environmental Issue', 'ENVIRONMENTAL_ISSUE', 'Waste, emissions, or environmental concern', 5, 'HIGH')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 4: SLA POLICIES
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS sla_policies (
    sla_policy_id BIGSERIAL PRIMARY KEY,
    severity_level VARCHAR(20) NOT NULL UNIQUE, -- CRITICAL, HIGH, MEDIUM, LOW
    target_resolution_minutes INTEGER NOT NULL,
    escalation_minutes INTEGER,
    priority_level VARCHAR(5) NOT NULL, -- P1, P2, P3, P4
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sla_policies_severity ON sla_policies(severity_level);

-- Insert SLA policies
INSERT INTO sla_policies (severity_level, target_resolution_minutes, escalation_minutes, priority_level, description)
VALUES 
    ('CRITICAL', 30, 10, 'P1', 'Critical - immediate response required'),
    ('HIGH', 240, 60, 'P2', 'High - 4 hour target resolution'),
    ('MEDIUM', 1440, 480, 'P3', 'Medium - 24 hour target resolution'),
    ('LOW', 4320, 1440, 'P4', 'Low - 72 hour target resolution')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 5: TICKETS (Core Incident Tickets)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tickets (
    ticket_id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE, -- INC-1001, INC-1002, etc.
    worker_id BIGINT NOT NULL,
    original_message TEXT NOT NULL,
    incident_type_id BIGINT NOT NULL,
    severity VARCHAR(20) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW
    department_id BIGINT NOT NULL,
    assigned_to_user_id BIGINT,
    priority VARCHAR(5) NOT NULL, -- P1, P2, P3, P4
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED
    sla_target_minutes INTEGER NOT NULL,
    sla_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    suggested_actions TEXT[], -- Array of action suggestions from AI
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (worker_id) REFERENCES users(user_id),
    FOREIGN KEY (incident_type_id) REFERENCES incident_types(incident_type_id),
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    FOREIGN KEY (assigned_to_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_tickets_number ON tickets(ticket_number);
CREATE INDEX idx_tickets_worker_id ON tickets(worker_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_department_id ON tickets(department_id);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to_user_id);
CREATE INDEX idx_tickets_sla_deadline ON tickets(sla_deadline);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_severity ON tickets(severity);
CREATE INDEX idx_tickets_status_sla ON tickets(status, sla_deadline);

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 6: TICKET COMMENTS (Audit Trail & Communication)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS ticket_comments (
    comment_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment_text TEXT NOT NULL,
    comment_type VARCHAR(50) NOT NULL DEFAULT 'NOTE', -- NOTE, STATUS_CHANGE, ASSIGNMENT, RESOLUTION
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_comments_ticket_id ON ticket_comments(ticket_id);
CREATE INDEX idx_comments_user_id ON ticket_comments(user_id);
CREATE INDEX idx_comments_created_at ON ticket_comments(created_at);

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 7: INCIDENT HISTORY (Audit Log)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS incident_history (
    history_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL, -- CREATED, ASSIGNED, UPDATED, RESOLVED, CLOSED
    changed_fields JSONB, -- JSON tracking what fields changed
    old_value TEXT, -- Previous value
    new_value TEXT, -- New value
    changed_by_user_id BIGINT,
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by_user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_history_ticket_id ON incident_history(ticket_id);
CREATE INDEX idx_history_event_type ON incident_history(event_type);
CREATE INDEX idx_history_timestamp ON incident_history(event_timestamp);

-- ═══════════════════════════════════════════════════════════════════════════════
-- TABLE 8: AI CLASSIFICATION CACHE (Performance Optimization)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS ai_classification_cache (
    cache_id BIGSERIAL PRIMARY KEY,
    message_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA256 hash of original message
    original_message TEXT NOT NULL,
    incident_type_id BIGINT,
    severity VARCHAR(20),
    priority VARCHAR(5),
    confidence_score DECIMAL(3,2),
    cached_response JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    hit_count INTEGER DEFAULT 0,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (incident_type_id) REFERENCES incident_types(incident_type_id)
);

CREATE INDEX idx_cache_message_hash ON ai_classification_cache(message_hash);

-- ═══════════════════════════════════════════════════════════════════════════════
-- SAMPLE DATA: DEPARTMENTS
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO departments (department_name, department_code, description, email, slack_channel)
VALUES
    ('Administration', 'ADMIN', 'Administrative office', 'admin@factory.com', '#admin'),
    ('Production / Maintenance', 'MAINTENANCE', 'Factory floor equipment maintenance', 'maintenance@factory.com', '#maintenance'),
    ('Occupational Health & Safety', 'SAFETY', 'Worker safety and compliance', 'safety@factory.com', '#safety'),
    ('Quality Assurance', 'QUALITY', 'Product quality control', 'quality@factory.com', '#quality'),
    ('Environmental / Waste Management', 'ENVIRONMENT', 'Environmental compliance', 'env@factory.com', '#environment')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- SAMPLE DATA: USERS
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO users (worker_id, full_name, email, phone, role)
VALUES
    ('WORKER-001', 'John Doe', 'john.doe@factory.com', '555-0101', 'WORKER'),
    ('WORKER-002', 'Jane Smith', 'jane.smith@factory.com', '555-0102', 'WORKER'),
    ('SUPERVISOR-001', 'Bob Johnson', 'bob.johnson@factory.com', '555-0201', 'SUPERVISOR'),
    ('MANAGER-001', 'Alice Williams', 'alice.williams@factory.com', '555-0301', 'MANAGER'),
    ('ADMIN-001', 'Charlie Brown', 'charlie.brown@factory.com', '555-0401', 'ADMIN')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- VIEWS (For Reporting & Dashboard)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Open incidents view
CREATE OR REPLACE VIEW v_open_incidents AS
SELECT 
    t.ticket_number,
    t.original_message,
    it.type_name as incident_type,
    t.severity,
    t.priority,
    d.department_name,
    u.full_name as assigned_to,
    t.sla_deadline,
    EXTRACT(MINUTE FROM (t.sla_deadline - CURRENT_TIMESTAMP)) as minutes_until_sla,
    t.created_at,
    t.status
FROM tickets t
JOIN incident_types it ON t.incident_type_id = it.incident_type_id
JOIN departments d ON t.department_id = d.department_id
LEFT JOIN users u ON t.assigned_to_user_id = u.user_id
WHERE t.status IN ('OPEN', 'IN_PROGRESS')
ORDER BY t.sla_deadline ASC;

-- SLA compliance view
CREATE OR REPLACE VIEW v_sla_compliance AS
SELECT 
    t.ticket_number,
    t.severity,
    sp.target_resolution_minutes,
    t.sla_deadline,
    CASE 
        WHEN t.status = 'RESOLVED' THEN 
            EXTRACT(MINUTE FROM (t.resolved_at - t.created_at))
        ELSE 
            EXTRACT(MINUTE FROM (CURRENT_TIMESTAMP - t.created_at))
    END as actual_resolution_minutes,
    CASE 
        WHEN t.status = 'RESOLVED' THEN
            CASE WHEN EXTRACT(MINUTE FROM (t.resolved_at - t.created_at)) <= sp.target_resolution_minutes 
                 THEN 'MET' ELSE 'BREACHED' END
        ELSE
            CASE WHEN CURRENT_TIMESTAMP > t.sla_deadline THEN 'BREACHED' ELSE 'ON_TRACK' END
    END as sla_status
FROM tickets t
JOIN sla_policies sp ON t.severity = sp.severity_level
ORDER BY t.sla_deadline DESC;

-- ═══════════════════════════════════════════════════════════════════════════════
-- STORED PROCEDURE: Generate Next Ticket Number
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION generate_ticket_number()
RETURNS VARCHAR AS $$
DECLARE
    next_number INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(ticket_number, 5) AS INT)), 1000) + 1
    INTO next_number
    FROM tickets;
    
    RETURN 'INC-' || next_number;
END;
$$ LANGUAGE plpgsql;

-- ═══════════════════════════════════════════════════════════════════════════════
-- STORED PROCEDURE: Update Department Foreign Key For Users
-- ═══════════════════════════════════════════════════════════════════════════════

ALTER TABLE users ADD CONSTRAINT fk_users_department 
    FOREIGN KEY (department_id) REFERENCES departments(department_id);

-- Update sample users to departments
UPDATE users SET department_id = 1 WHERE worker_id = 'WORKER-001';
UPDATE users SET department_id = 2 WHERE worker_id = 'WORKER-002';
UPDATE users SET department_id = 2 WHERE worker_id = 'SUPERVISOR-001';
UPDATE users SET department_id = 1 WHERE worker_id = 'MANAGER-001';
UPDATE users SET department_id = 1 WHERE worker_id = 'ADMIN-001';

-- ═══════════════════════════════════════════════════════════════════════════════
-- GRANTS & PERMISSIONS (Example - adjust as needed)
-- ═══════════════════════════════════════════════════════════════════════════════

-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO app_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- ═══════════════════════════════════════════════════════════════════════════════
-- END OF SCHEMA
-- ═══════════════════════════════════════════════════════════════════════════════
