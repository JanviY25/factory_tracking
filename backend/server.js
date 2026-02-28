const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ================= DATABASE CONNECTION =================
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: 'dheeraj@123',
    database: 'factory_tracking'
});

db.connect((err) => {
    if (err) {
        console.error("❌ Database connection failed:", err.message);
    } else {
        console.log("✅ Database connected successfully");
    }
});

// ================= 1. LOGIN & AUTHENTICATION =================

// Supervisor Login
// IMPROVED: Now returns active shift info if the supervisor is already working
app.post('/api/login', (req, res) => {
    const { userId, password } = req.body;
    const sql = "SELECT * FROM supervisors WHERE supervisor_id=? AND password=?";
    db.query(sql, [userId, password], (err, results) => {
        if (err) return res.json({ status: "fail", message: "DB Error" });
        if (results && results.length > 0) {
            const supervisor = results[0];

            // Check if this supervisor has an active shift already in the DB
            const shiftSql = "SELECT session_id, line_id, shift, end_time FROM shift_sessions WHERE supervisor_id=? AND status='active' LIMIT 1";
            db.query(shiftSql, [userId], (err2, shiftResults) => {
                let activeShift = null;
                if (shiftResults && shiftResults.length > 0) {
                    activeShift = shiftResults[0];
                }

                res.json({
                    status: "success",
                    supervisor_id: supervisor.supervisor_id,
                    name: supervisor.name,
                    line: supervisor.line, // default line from master
                    active_session: activeShift // Can be null if no active shift
                });
            });
        } else {
            res.json({ status: "fail", message: "Invalid credentials" });
        }
    });
});

// Admin Login
app.post('/api/admin/login', (req, res) => {
    const { userId, password } = req.body;
    const sql = "SELECT * FROM admins WHERE admin_id=? AND password=?";
    db.query(sql, [userId, password], (err, results) => {
        if (err) return res.json({ status: "fail" });
        if (results && results.length > 0) {
            res.json({ status: "success", admin_id: results[0].admin_id });
        } else {
            res.json({ status: "fail" });
        }
    });
});

// Operator Login
app.post('/api/operator/login', (req, res) => {
    const { name, password } = req.body;
    if (password !== '12345') return res.json({ status: 'fail', message: 'Invalid password' });
    db.query("SELECT operator_id, name FROM operators WHERE name = ?", [name], (err, results) => {
        if (err) return res.status(500).json({ status: 'error' });
        if (!results || results.length === 0) {
            return res.json({ status: 'fail', message: "Operator not found" });
        }
        res.json({ status: 'success', operator_id: results[0].operator_id, name: results[0].name });
    });
});

// ================= 2. LINE & STATION MANAGEMENT =================

app.post('/api/admin/addLine', (req, res) => {
    const { lineName, stationCount } = req.body;
    if (!lineName || !stationCount) return res.json({ status: "fail" });

    const stations = [];
    for (let i = 1; i <= stationCount; i++) {
        const sId = `${lineName.replace(/\s+/g, '')}-ST${i.toString().padStart(2, '0')}`;
        stations.push([sId, lineName, null, null, 'red']);
    }

    const sql = "INSERT INTO stations (station_id, line, operator_id, operator_name, status) VALUES ?";
    db.query(sql, [stations], (err) => {
        if (err) return res.json({ status: "fail", message: err.message });
        res.json({ status: "success" });
    });
});

app.post('/api/admin/removeLine', (req, res) => {
    const { lineName } = req.body;
    db.query("DELETE FROM stations WHERE line = ?", [lineName], (err) => {
        if (err) return res.json({ status: "fail" });
        res.json({ status: "success" });
    });
});

app.get('/api/lines', (req, res) => {
    db.query("SELECT DISTINCT line FROM stations ORDER BY line", (err, results) => {
        if (err) return res.json({ status: "fail", lines: [] });
        res.json({ status: "success", lines: (results || []).map(row => row.line) });
    });
});

app.post('/api/admin/toggleMaintenance', (req, res) => {
    const { station_id, isMaintenance } = req.body;
    const status = isMaintenance ? 'maintenance' : 'red';
    const sql = "UPDATE stations SET status=?, operator_id=NULL, operator_name=NULL WHERE station_id=?";
    db.query(sql, [status, station_id], (err) => {
        if (err) return res.json({ status: "fail" });
        res.json({ status: "success" });
    });
});

// ================= 3. SHIFT CONTROL & AUTO-EXPIRY =================

app.post('/api/startShift', (req, res) => {
    const { supervisor_id, line_id, shift, end_time } = req.body;
    const requestedLines = line_id.split(',').map(l => l.trim());

    // Check if any line is already being used in an active shift by ANOTHER supervisor
    db.query("SELECT ss.line_id, sv.name, ss.supervisor_id FROM shift_sessions ss JOIN supervisors sv ON ss.supervisor_id = sv.supervisor_id WHERE ss.status='active'", (err, activeSessions) => {
        if (err) return res.json({ status: "fail", message: "Error checking active shifts" });

        for (let session of activeSessions) {
            // Allow if it's the SAME supervisor (let them resume/restart)
            if (session.supervisor_id === supervisor_id) continue;

            const activeLineList = session.line_id.split(',').map(l => l.trim());
            for (let rLine of requestedLines) {
                if (activeLineList.includes(rLine)) {
                    return res.json({ status: "fail", message: `Line ${rLine} is already being managed by supervisor ${session.name}.` });
                }
            }
        }

        const sql = `INSERT INTO shift_sessions (supervisor_id, line_id, shift, start_time, end_time, status) VALUES (?, ?, ?, NOW(), ?, 'active')`;
        db.query(sql, [supervisor_id, line_id, shift, end_time], (err2, result) => {
            if (err2) {
                console.error("StartShift Error:", err2);
                return res.json({ status: "fail", message: err2.message });
            }
            res.json({ status: "success", session_id: result.insertId });
        });
    });
});

app.post('/api/updateShiftTime', (req, res) => {
    const { session_id, new_end_time } = req.body;
    db.query("UPDATE shift_sessions SET end_time=? WHERE session_id=?", [new_end_time, session_id], (err) => {
        if (err) return res.json({ status: "fail" });
        res.json({ status: "success" });
    });
});

app.post('/api/endShift', (req, res) => {
    const { session_id, line_id } = req.body;
    const lines = line_id.split(',');
    db.query("UPDATE transactions SET end_time=NOW() WHERE line_id IN (?) AND end_time IS NULL", [lines]);
    db.query("UPDATE shift_sessions SET status='ended' WHERE session_id=?", [session_id]);
    db.query("UPDATE stations SET operator_id=NULL, operator_name=NULL, status='red' WHERE line IN (?) AND status != 'maintenance'", [lines]);
    res.json({ status: "success" });
});

setInterval(() => {
    const sql = "SELECT session_id, line_id FROM shift_sessions WHERE status='active' AND end_time < NOW()";
    db.query(sql, (err, sessions) => {
        if (sessions && sessions.length > 0) {
            sessions.forEach(s => {
                const lines = s.line_id.split(',');
                db.query("UPDATE transactions SET end_time=NOW() WHERE line_id IN (?) AND end_time IS NULL", [lines]);
                db.query("UPDATE shift_sessions SET status='ended' WHERE session_id=?", [s.session_id]);
                db.query("UPDATE stations SET operator_id=NULL, operator_name=NULL, status='red' WHERE line IN (?) AND status != 'maintenance'", [lines]);
            });
        }
    });
}, 60000);

// ================= 4. DASHBOARD LOGIC (KPI & COLORS) =================

async function getStationsWithCalculatedStatus(whereClause, params) {
    return new Promise((resolve, reject) => {
        const sql = `
            SELECT s.station_id, s.line, s.operator_id, s.operator_name, s.status as current_status,
            (SELECT COUNT(DISTINCT DATE(t.start_time)) FROM transactions t
             WHERE t.operator_id = s.operator_id AND t.station_id = s.station_id) as days_worked
            FROM stations s ${whereClause} ORDER BY s.line, s.station_id`;

        db.query(sql, params, (err, results) => {
            if (err) return reject(err);
            const processed = (results || []).map(row => {
                let status = row.current_status;
                if (status !== 'maintenance') {
                    status = row.operator_id ? (row.days_worked >= 3 ? 'green' : 'yellow') : 'red';
                }
                return { station_id: row.station_id, line: row.line, operator_id: row.operator_id, operator_name: row.operator_name, status: status };
            });
            resolve(processed);
        });
    });
}

app.post('/api/getStations', async (req, res) => {
    const { line } = req.body;
    try {
        const stations = await getStationsWithCalculatedStatus("WHERE s.line = ?", [line]);
        res.json({ status: "success", stations });
    } catch (err) { res.json({ status: "fail" }); }
});

app.get('/api/admin/lines', async (req, res) => {
    // Only return stations for lines that have an ACTIVE shift session
    const activeSessionsSql = `SELECT ss.line_id, sv.name as supervisor_name FROM shift_sessions ss
                               JOIN supervisors sv ON ss.supervisor_id = sv.supervisor_id
                               WHERE ss.status='active'`;

    db.query(activeSessionsSql, async (err, activeSessions) => {
        if (err || !activeSessions || activeSessions.length === 0) {
            return res.json({ status: "success", stations: [], message: "No active lines working" });
        }

        let activeLineList = [];
        const lineMap = {};
        activeSessions.forEach(s => {
            const lines = s.line_id.split(',').map(l => l.trim());
            lines.forEach(l => {
                lineMap[l] = s.supervisor_name;
                activeLineList.push(l);
            });
        });

        try {
            const stations = await getStationsWithCalculatedStatus("WHERE s.line IN (?)", [activeLineList]);
            // Attach supervisor info to each station for admin view
            const stationData = stations.map(station => ({
                ...station,
                in_charge: lineMap[station.line] || "N/A"
            }));
            res.json({ status: "success", stations: stationData });
        } catch (err) {
            res.json({ status: "fail", stations: [] });
        }
    });
});

// ================= 5. SCANNING, VALIDATION & TRANSACTIONS =================

app.post('/api/assign', (req, res) => {
    const { station_id, operator_id, operator_name, supervisor_id, shift } = req.body;

    const checkSql = "SELECT station_id FROM stations WHERE operator_id = ?";
    db.query(checkSql, [operator_id], (err, rows) => {
        if (rows && rows.length > 0 && rows[0].station_id !== station_id) {
            db.query("INSERT INTO validation_failures (supervisor_id, station_id, operator_id, reason, fail_time) VALUES (?,?,?,?,NOW())",
                [supervisor_id, station_id, operator_id, `Operator already at ${rows[0].station_id}`]);

            return res.json({ status: "fail", message: `Operator already assigned to ${rows[0].station_id}` });
        }

        db.query("SELECT line FROM stations WHERE station_id = ?", [station_id], (errLine, rowsLine) => {
            if (!rowsLine || rowsLine.length === 0) return res.json({ status: "fail", message: "Station not found" });
            const line_id = rowsLine[0].line;

            db.query("UPDATE transactions SET end_time=NOW() WHERE station_id=? AND end_time IS NULL", [station_id], () => {
                const insSql = `INSERT INTO transactions (operator_id, station_id, line_id, supervisor_id, shift, start_time) VALUES (?,?,?,?,?,NOW())`;
                db.query(insSql, [operator_id, station_id, line_id, supervisor_id, shift], () => {
                    db.query("UPDATE stations SET operator_id=?, operator_name=? WHERE station_id=?", [operator_id, operator_name, station_id]);
                    res.json({ status: "success" });
                });
            });
        });
    });
});

// ================= 6. ANALYTICS & REPORTS =================

app.get('/api/admin/analytics', (req, res) => {
    const sql = `SELECT
        (SELECT COUNT(*) FROM stations WHERE operator_id IS NOT NULL) as active_ops,
        (SELECT COUNT(*) FROM stations) as total_stations,
        (SELECT COUNT(*) FROM validation_failures) as fail_count,
        (SELECT COUNT(*) FROM transactions) as success_count`;
    db.query(sql, (err, results) => {
        const r = results[0];
        const rate = r.success_count > 0 ? ((r.success_count / (r.success_count + r.fail_count)) * 100).toFixed(1) : 100;
        res.json({ status: "success", data: { ...r, success_rate: rate + "%" } });
    });
});

app.get('/api/export/csv', (req, res) => {
    const { startDate, endDate } = req.query;
    const sql = `SELECT * FROM transactions WHERE DATE(start_time) BETWEEN ? AND ? ORDER BY start_time`;
    db.query(sql, [startDate || '2000-01-01', endDate || '2099-12-31'], (err, rows) => {
        const header = 'txn_id,operator_id,station_id,line_id,supervisor_id,shift,start_time,end_time\n';
        const csv = header + (rows || []).map(r => [r.txn_id, r.operator_id, r.station_id, r.line_id, r.supervisor_id, r.shift, r.start_time, r.end_time].join(',')).join('\n');
        res.setHeader('Content-Type', 'text/csv');
        res.send(csv);
    });
});

// ================= 7. MASTER DATA MANAGEMENT =================

app.get('/api/admin/supervisors', (req, res) => {
    db.query("SELECT supervisor_id, name, line FROM supervisors", (err, results) => {
        res.json({ status: "success", list: results || [] });
    });
});

app.post('/api/admin/supervisors', (req, res) => {
    const { supervisor_id, name, password, line } = req.body;
    db.query("INSERT INTO supervisors (supervisor_id, name, password, line) VALUES (?, ?, ?, ?)",
        [supervisor_id, name || supervisor_id, password, line], (err) => {
        res.json({ status: err ? "fail" : "success" });
    });
});

app.delete('/api/admin/supervisors/:id', (req, res) => {
    db.query("DELETE FROM supervisors WHERE supervisor_id = ?", [req.params.id], (err) => {
        res.json({ status: err ? "fail" : "success" });
    });
});

// Operator Work History
app.post('/api/operator/history', (req, res) => {
    const { operator_id } = req.body;
    const sql = `SELECT station_id, shift, DATE(start_time) as date FROM transactions WHERE operator_id = ? ORDER BY start_time DESC`;
    db.query(sql, [operator_id], (err, results) => {
        if (err) return res.status(500).json({ status: 'error' });
        if (!results || results.length === 0) {
            return res.json({ status: 'success', history: [], message: "Sorry, no work is assigned to you until now." });
        }
        const history = results.map(row => ({
            station_id: row.station_id,
            date: row.date ? new Date(row.date).toISOString().split('T')[0] : 'Unknown',
            shift: row.shift || 'N/A'
        }));
        res.json({ status: 'success', history });
    });
});

// ================= SERVER START =================
const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Factory Tracking Server running on port ${PORT}`);
});
