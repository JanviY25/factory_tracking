# Factory Tracking

Android (Java) app for factory supervisors to manage shifts, scan station/operator QR codes, and view live dashboards. Admin can manage supervisors and export transaction CSV. Node.js backend with MySQL.

## Backend (Node.js + MySQL)

### 1. Database

Create database and run the schema (extends your existing schema with `shift_sessions`, `admins`, and `stations.operator_id`/`status`, `transactions.line_id`):

```bash
mysql -u root -p
```

```sql
CREATE DATABASE factory_tracking;
USE factory_tracking;
SOURCE backend/database/schema.sql;
```

Or run the SQL in `backend/database/schema.sql` manually. If you already have `stations` without `operator_id`/`status`, run:

```sql
ALTER TABLE stations ADD COLUMN operator_id VARCHAR(20) DEFAULT NULL;
ALTER TABLE stations ADD COLUMN status VARCHAR(20) DEFAULT 'red';
ALTER TABLE transactions ADD COLUMN line_id VARCHAR(50);
```

### 2. Server

```bash
cd backend
npm install
# Edit server.js if needed: set MySQL host, user, password
npm start
```

Server runs on `http://0.0.0.0:3000`. APIs are under `/api/`:

- `POST /api/login` — supervisor login (body: `userId`, `password`)
- `POST /api/admin/login` — admin login
- `POST /api/startShift` — start shift (body: `supervisor_id`, `line_id`, `shift`, `end_time`)
- `POST /api/getStations` — stations by line (body: `line`)
- `GET /api/admin/lines` — all stations (admin dashboard)
- `POST /api/assign` — assign operator to station (body: `station_id`, `operator_id`, `supervisor_id`, `shift`)
- `POST /api/endShift` — end shift (body: `session_id`, `line_id`)
- `GET /api/transactions` — list transactions
- `GET /api/export/csv?date=YYYY-MM-DD` — export day's transactions as CSV
- `GET /api/admin/supervisors` — list supervisors
- `POST /api/admin/supervisors` — add supervisor
- `DELETE /api/admin/supervisors/:id` — delete supervisor
- `POST /api/operator/login`, `POST /api/operator/history` — operator login and work history

## Android App

- **Retrofit** for all API calls; **SharedPreferences** for session (supervisor/admin and shift session).
- **RecyclerView** for supervisor dashboard (stations) and admin dashboard (all lines).

### Flow

1. **Login**  
   Set optional Server URL (e.g. `http://192.168.1.4:3000/` for device).  
   - **Supervisor Login** → Start Shift screen (line from account, shift name, start/end time) → **Start Shift** → Dashboard.  
   - **Admin Login** → Admin dashboard (all lines, manage supervisors, export CSV).  
   - **Operator Login** → Operator work history.

2. **Supervisor**  
   - **Start Shift**: choose shift name and end time, then Start Shift.  
   - **Dashboard**: stations for supervisor’s line, refreshed every 2 seconds; **Scan QR** opens scanner screen; **End Shift** ends session and logs out.  
   - **Scan**: hidden, auto-focused input for Bluetooth/hardware QR scanner. Scan **station** first (e.g. `STATION ID: LINE1-STO5` → station `LINE1-STO5`), then **operator** (e.g. `OPERATOR ID: OP-001` → operator `OP-001`). App sends assign to backend and shows status.

3. **Admin**  
   - View all lines/stations (polling every 2 s).  
   - **Manage Supervisors**: add (ID, name, password, line) and delete.  
   - **Export today's CSV**: downloads today’s transactions and opens with FileProvider.

### QR format

- Station: `STATION ID: LINE1-STO5` (or plain `LINE1-STO5`).  
- Operator: `OPERATOR ID: OP-001` (or plain `OP-001`).  
Scanner field is hidden and keyboard suppressed so hardware scanners work as keyboard input.

### Data

- All data persists in MySQL until you delete it. Session in the app persists until **End Shift** (supervisor) or **Logout** (admin).

## Build

```bash
# Android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
