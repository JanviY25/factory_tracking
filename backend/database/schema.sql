-- Factory Tracking Database Schema
-- Run this after creating database: CREATE DATABASE factory_tracking; USE factory_tracking;

-- Operators
CREATE TABLE IF NOT EXISTS operators (
    operator_id VARCHAR(20),
    name VARCHAR(100),
    contractor VARCHAR(100)
);

-- Stations: add operator_id and status for live dashboard
CREATE TABLE IF NOT EXISTS stations (
    station_id VARCHAR(20) PRIMARY KEY,
    line VARCHAR(50),
    operator_id VARCHAR(20) DEFAULT NULL,
    operator_name VARCHAR(100) DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'red'
);

-- Supervisors
CREATE TABLE IF NOT EXISTS supervisors (
    supervisor_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100),
    password VARCHAR(100),
    line VARCHAR(50)
);

-- Admin users (for admin dashboard, CRUD supervisors, CSV export)
CREATE TABLE IF NOT EXISTS admins (
    admin_id VARCHAR(20) PRIMARY KEY,
    password VARCHAR(100)
);

-- Shift sessions (supervisor starts a shift with line and end time)
CREATE TABLE IF NOT EXISTS shift_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    supervisor_id VARCHAR(20),
    line_id VARCHAR(50),
    shift VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    status VARCHAR(20) DEFAULT 'active'
);

-- Transactions: add line_id for easier end-shift queries
CREATE TABLE IF NOT EXISTS transactions (
    txn_id INT AUTO_INCREMENT PRIMARY KEY,
    operator_id VARCHAR(20),
    station_id VARCHAR(20),
    line_id VARCHAR(50),
    supervisor_id VARCHAR(20),
    shift VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME
);

-- Process Completions (Req 2.4)
CREATE TABLE IF NOT EXISTS process_completions (
    process_id INT AUTO_INCREMENT PRIMARY KEY,
    operator_id VARCHAR(20),
    station_id VARCHAR(20),
    step_name VARCHAR(100),
    completion_time DATETIME
);

-- Validation Failures (Req 4)
CREATE TABLE IF NOT EXISTS validation_failures (
    fail_id INT AUTO_INCREMENT PRIMARY KEY,
    supervisor_id VARCHAR(20),
    station_id VARCHAR(20),
    operator_id VARCHAR(20),
    reason VARCHAR(100),
    fail_time DATETIME
);

-- If stations already exists without operator_id/status, run:
-- ALTER TABLE stations ADD COLUMN operator_id VARCHAR(20) DEFAULT NULL;
-- ALTER TABLE stations ADD COLUMN status VARCHAR(20) DEFAULT 'red';

-- Sample data
INSERT INTO stations (station_id, line, operator_id, status) VALUES
('LINE1-ST01', 'LINE1', NULL, 'red'),
('LINE1-ST02', 'LINE1', NULL, 'red'),
('LINE1-ST03', 'LINE1', NULL, 'red'),
('LINE1-ST04', 'LINE1', NULL, 'red'),
('LINE1-ST05', 'LINE1', NULL, 'red')
ON DUPLICATE KEY UPDATE line = VALUES(line);

INSERT INTO supervisors (supervisor_id, name, password, line) VALUES
('SV001', 'Supervisor One', 'password123', 'LINE1'),
('SV002', 'Supervisor Two', 'password123', 'LINE1')
ON DUPLICATE KEY UPDATE name = VALUES(name), password = VALUES(password), line = VALUES(line);

INSERT INTO admins (admin_id, password) VALUES
('ADMIN', 'admin123');

INSERT INTO operators (operator_id, name, contractor) VALUES
('OP001', 'John Doe', 'Contractor A'),
('OP002', 'Jane Smith', 'Contractor B'),
('OP003', 'Mike Johnson', 'Contractor A'),
('OP004', 'Sarah Williams', 'Contractor B'),
('OP005', 'Tom Brown', 'Contractor A')
ON DUPLICATE KEY UPDATE name = VALUES(name), contractor = VALUES(contractor);
