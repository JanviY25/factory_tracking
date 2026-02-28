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
        console.log("Database connection failed", err);
    } else {
        console.log("Database connected successfully");
    }
});

// ================= LOGIN & MASTER DATA =================
app.post('/api/login', (req, res) => {
    const { userId, password } = req.body;
    const sql = "SELECT * FROM supervisors WHERE supervisor_id=? AND password=?";
    db.query(sql, [userId, password], (err, results) => {
        if (results && results.length > 0) {
            res.json({
                status: "success",
                supervisor_id: results[0].supervisor_id,
                name: results[0].name,
                line: results[0].line
            });
        } else {
            res.json({ status: "fail", message: "Invalid credentials" });
        }
    });
});

app.post('/api/admin/login', (req, res) => {
    const { userId, password } = req.body;
    const sql = "SELECT * FROM admins WHERE admin_id=? AND password=?";
    db.query(sql, [userId, password], (err, results) => {
        if (results && results.length > 0) {
            res.json({ status: "success", admin_id: results[0].admin_id });
        } else {
            res.json({ status: "fail" });
        }
    });
});

// ================= LINE & STATION MANAGEMENT =================
app.post('/api/admin/addLine', (req, res) => {
    const { lineName, stationCount } = req.body;
    const stations = [];
    for (let i = 1; i <= stationCount; i++) {
        stations.push([`${lineName.replace(/\s+/g, '')}-ST${i.toString().padStart(2, '0')}`, lineName, null, 'red']);
    }
    db.query("INSERT INTO stations (station_id, line, operator_id, status) VALUES ?", [stations], (err) => {
        res.json({ status: err ? "fail" : "success" });
    });
});

app.post('/api/admin/removeLine', (req, res) => {
    const { lineName } = req.body;
    db.query("DELETE FROM stations WHERE line = ?", [lineName], (err) => {
        res.json({ status: err ? "fail" : "success" });
    });
});

app.get('/api/lines', (req, res) => {
    db.query("SELECT DISTINCT line FROM stations ORDER BY line", (err, results) => {
        res.json({ status: "success", lines: (results || []).map(row => row.line) });
    });
});

// ================= SCANNING & VALIDATION =================
app.post('/api/assign', (req, res) => {
    const { station_id, operator_id, supervisor_id, shift } = req.body;

    // Validate operator assigned elsewhere
    db.query("SELECT station_id FROM stations WHERE operator_id = ?", [operator_id], (err, rows) => {
        if (rows && rows.length > 0 && rows[0].station_id !== station_id) {
            return res.json({ status: "fail", message: `Operator already at ${rows[0].station_id}` });
        }

        db.query("SELECT line FROM stations WHERE station_id = ?", [station_id], (errLine, rowsLine) => {
            if (!rowsLine || rowsLine.length === 0) return res.json({ status: "fail", message: "Station not found" });
            const line_id = rowsLine[0].line;

            db.query("UPDATE transactions SET end_time=NOW() WHERE station_id=? AND end_time IS NULL", [station_id], () => {
                const insertSql = `INSERT INTO transactions (operator_id, station_id, line_id, supervisor_id, shift, start_time) VALUES (?,?,?,?,?,NOW())`;
                db.query(insertSql, [operator_id, station_id, line_id, supervisor_id, shift], () => {
                    db.query("UPDATE stations SET operator_id=? WHERE station_id=?", [operator_id, station_id]);
                    res.json({ status: "success" });
                });
            });
        });
    });
});

// ================= SHIFT CONTROL & AUTO-OUT =================
app.post('/api/endShift', (req, res) => {
    const { session_id, line_id } = req.body;
    const lines = line_id.split(',');
    db.query("UPDATE transactions SET end_time=NOW() WHERE line_id IN (?) AND end_time IS NULL", [lines]);
    db.query("UPDATE shift_sessions SET status='ended' WHERE session_id=?", [session_id]);
    db.query("UPDATE stations SET operator_id=NULL WHERE line IN (?)", [lines]);
    res.json({ status: "success" });
});

// Auto-expire task runs every minute
setInterval(() => {
    db.query("SELECT session_id, line_id FROM shift_sessions WHERE status='active' AND end_time < NOW()", (err, sessions) => {
        if (sessions) sessions.forEach(s => {
            const lines = s.line_id.split(',');
            db.query("UPDATE transactions SET end_time=NOW() WHERE line_id IN (?) AND end_time IS NULL", [lines]);
            db.query("UPDATE shift_sessions SET status='ended' WHERE session_id=?", [s.session_id]);
            db.query("UPDATE stations SET operator_id=NULL WHERE line IN (?)", [lines]);
        });
    });
}, 60000);

// ================= REPORTS & KPI =================
app.get('/api/admin/analytics', (req, res) => {
    const sql = `SELECT
        (SELECT COUNT(*) FROM stations WHERE operator_id IS NOT NULL) as active_ops,
        (SELECT COUNT(*) FROM stations) as total_stations,
        (SELECT COUNT(*) FROM transactions) as success_count`;
    db.query(sql, (err, results) => {
        res.json({ status: "success", data: results[0] });
    });
});

app.get('/api/export/csv', (req, res) => {
    const { startDate, endDate } = req.query;
    const start = startDate || '2000-01-01';
    const end = endDate || '2099-12-31';
    db.query("SELECT * FROM transactions WHERE DATE(start_time) BETWEEN ? AND ? ORDER BY start_time", [start, end], (err, rows) => {
        const header = 'txn_id,operator_id,station_id,line_id,supervisor_id,shift,start_time,end_time\n';
        const csv = header + (rows || []).map(r => [r.txn_id, r.operator_id, r.station_id, r.line_id, r.supervisor_id, r.shift, r.start_time, r.end_time].join(',')).join('\n');
        res.setHeader('Content-Type', 'text/csv');
        res.send(csv);
    });
});

// ================= OPERATOR HISTORY & LOGIN =================
app.post('/api/operator/history', (req, res) => {
    const { operator_id } = req.body;
    const sql = `SELECT station_id, shift, DATE(start_time) as date FROM transactions WHERE operator_id = ? ORDER BY start_time DESC`;
    db.query(sql, [operator_id], (err, results) => {
        if (err) return res.status(500).json({ status: 'error' });
        const history = (results || []).map(row => ({
            station_id: row.station_id,
            date: row.date ? new Date(row.date).toISOString().split('T')[0] : 'Unknown',
            shift: row.shift || 'N/A'
        }));
        res.json({ status: 'success', history });
    });
});

app.post('/api/operator/login', (req, res) => {
    const { name, password } = req.body;
    if (password !== '12345') return res.json({ status: 'fail', message: 'Invalid password' });
    db.query("SELECT operator_id, name FROM operators WHERE name = ?", [name], (err, results) => {
        if (err || results.length === 0) return res.json({ status: 'fail', message: 'Operator not found' });
        res.json({ status: 'success', operator_id: results[0].operator_id, name: results[0].name });
    });
});

// ================= DASHBOARD & STATUS LOGIC =================
async function getStationsWithCalculatedStatus(whereClause, params) {
    return new Promise((resolve, reject) => {
        const sql = `
            SELECT s.station_id, s.line, s.operator_id, s.status as current_status,
            (SELECT COUNT(DISTINCT DATE(t.start_time)) FROM transactions t WHERE t.operator_id = s.operator_id AND t.station_id = s.station_id) as days_worked
            FROM stations s ${whereClause} ORDER BY s.line, s.station_id`;

        db.query(sql, params, (err, results) => {
            if (err) return reject(err);
            const processed = results.map(row => {
                let status = row.current_status === 'maintenance' ? 'maintenance' : (row.operator_id ? (row.days_worked >= 3 ? 'green' : 'yellow') : 'red');
                return { station_id: row.station_id, line: row.line, operator_id: row.operator_id, status: status };
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
    try {
        const stations = await getStationsWithCalculatedStatus("", []);
        res.json({ status: "success", stations });
    } catch (err) { res.json({ status: "fail", stations: [] }); }
});

// ================= MASTER DATA (SUPERVISORS) =================
app.get('/api/admin/supervisors', (req, res) => {
    db.query("SELECT supervisor_id, name, line FROM supervisors", (err, results) => {
        if (err) return res.json({ status: "fail", list: [] });
        res.json({ status: "success", list: results });
    });
});

app.post('/api/admin/supervisors', (req, res) => {
    const { supervisor_id, name, password, line } = req.body;
    db.query("INSERT INTO supervisors (supervisor_id, name, password, line) VALUES (?, ?, ?, ?)", [supervisor_id, name, password, line], (err) => {
        if (err) return res.json({ status: "fail" });
        res.json({ status: "success" });
    });
});

app.delete('/api/admin/supervisors/:id', (req, res) => {
    db.query("DELETE FROM supervisors WHERE supervisor_id = ?", [req.params.id], (err) => {
        if (err) return res.json({ status: "fail" });
        res.json({ status: "success" });
    });
});

// ================= START SERVER =================
const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => { console.log(`Server running on port ${PORT}`); });
