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
    const res = await fetch(`${API_BASE}/admin/analytics`);
    const json = await res.json();
    if (json.status === 'success') {
        const d = json.data;
        document.getElementById('kpi-expert').textContent = d.expert_count;
        document.getElementById('kpi-learner').textContent = d.learner_count;
        document.getElementById('kpi-vacant').textContent = d.vacant_count;
    }
}

async function fetchLines() {
    const res = await fetch(`${API_BASE}/admin/lines`);
    const json = await res.json();
    if (json.status === 'success') {
        renderLines(json.stations);
    }
}

function renderLines(stations) {
    const container = document.getElementById('lines-container');
    container.innerHTML = '';

    // Group stations by line
    const linesMap = {};
    for (const st of stations) {
        if (!linesMap[st.line]) {
            linesMap[st.line] = [];
        }
        linesMap[st.line].push(st);
    }

    const lineNames = Object.keys(linesMap).sort();

    if (lineNames.length === 0) {
        container.innerHTML = '<p>No factory lines available.</p>';
        return;
    }

    lineNames.forEach(lineName => {
        const groupDiv = document.createElement('div');
        groupDiv.className = 'line-group';

        const title = document.createElement('h3');
        title.className = 'line-title';
        title.textContent = lineName;
        groupDiv.appendChild(title);

        const gridDiv = document.createElement('div');
        gridDiv.className = 'stations-grid';

        linesMap[lineName].forEach(st => {
            const stDiv = document.createElement('div');
            stDiv.className = `station status-${st.status}`;

            let html = `<span>${st.station_id}</span>`;
            if (st.operator_id) {
                html += `<span class="op-id">${st.operator_id}</span>`;
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
