const API_BASE = 'http://localhost:3000/api';

async function fetchData() {
    try {
        await Promise.all([
            fetchAnalytics(),
            fetchLines()
        ]);
    } catch (err) {
        console.error("Error fetching data:", err);
    }
}

async function fetchAnalytics() {
    try {
        const res = await fetch(`${API_BASE}/admin/analytics`);
        const json = await res.json();
        if (json.status === 'success') {
            const d = json.data;
            document.getElementById('kpi-expert').textContent = d.active_ops || 0;
            document.getElementById('kpi-learner').textContent = d.total_stations || 0;
            document.getElementById('kpi-vacant').textContent = d.success_rate || '100%';
        }
    } catch (err) {
        console.warn("Analytics endpoint error:", err);
    }
}

async function fetchLines() {
    const res = await fetch(`${API_BASE}/admin/lines`);
    const json = await res.json();
    if (json.status === 'success') {
        renderLines(json.stations, json.active_sessions || [], json.message);
    }
}

function renderLines(stations, activeSessions, message) {
    const container = document.getElementById('lines-container');
    container.innerHTML = '';

    // If no active lines, show the message
    if (!stations || stations.length === 0) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'no-active-msg';
        msgDiv.innerHTML = `
            <div style="text-align:center;padding:3rem;background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,0.05);">
                <h3 style="color:#666;margin:0 0 0.5rem 0;">📋 ${message || 'No active lines working'}</h3>
                <p style="color:#999;margin:0;">Supervisors need to start a shift for lines to appear here.</p>
            </div>`;
        container.appendChild(msgDiv);
        return;
    }

    // Build supervisor map from active_sessions
    const supervisorMap = {};
    activeSessions.forEach(s => {
        supervisorMap[s.line] = s;
    });

    // Group stations by line
    const linesMap = {};
    for (const st of stations) {
        if (!linesMap[st.line]) {
            linesMap[st.line] = [];
        }
        linesMap[st.line].push(st);
    }

    const lineNames = Object.keys(linesMap).sort();

    lineNames.forEach(lineName => {
        const groupDiv = document.createElement('div');
        groupDiv.className = 'line-group';

        const title = document.createElement('h3');
        title.className = 'line-title';

        // Show supervisor info for this line
        const svInfo = supervisorMap[lineName];
        if (svInfo) {
            title.innerHTML = `${lineName} <span style="float:right;font-size:0.85rem;font-weight:normal;color:#6200EA;">👷 ${svInfo.supervisor_name} (${svInfo.supervisor_id}) | Shift: ${svInfo.shift || 'N/A'}</span>`;
        } else {
            title.textContent = lineName;
        }
        groupDiv.appendChild(title);

        const gridDiv = document.createElement('div');
        gridDiv.className = 'stations-grid';

        linesMap[lineName].forEach(st => {
            const stDiv = document.createElement('div');
            stDiv.className = `station status-${st.status}`;

            let html = `<span>${st.station_id}</span>`;
            if (st.operator_id) {
                const displayName = st.operator_name || st.operator_id;
                html += `<span class="op-id">${displayName}</span>`;
                html += `<span class="op-id" style="font-size:0.7rem;opacity:0.8">${st.operator_id}</span>`;
            } else {
                html += `<span class="op-id">VACANT</span>`;
            }
            stDiv.innerHTML = html;
            gridDiv.appendChild(stDiv);
        });

        groupDiv.appendChild(gridDiv);
        container.appendChild(groupDiv);
    });
}

// Initial fetch
fetchData();

// Auto refresh every 5 seconds
setInterval(fetchData, 5000);

