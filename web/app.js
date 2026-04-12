/* ─────────────────────────────────────────
   TimeLog Web-Dashboard  –  app.js
   Echtzeit-Anzeige für Freddy's iPhone
───────────────────────────────────────── */

// ── State ────────────────────────────────────────
const state = {
  page:       'dashboard',
  projects:   [],
  workLogs:   [],
  timer:      null,
  timerTick:  null,  // setInterval für Live-Timer
  autoRefresh: null,  // setInterval für Daten-Reload
};

// ── Navigation ───────────────────────────────────
function navigate(name, btn) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.page').forEach(p  => p.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById(`page-${name}`).classList.add('active');
  state.page = name;

  if (name === 'projects') renderProjects();
  if (name === 'history')  loadHistory();
  if (name === 'dashboard') renderDashboard();
}

// ── API ──────────────────────────────────────────
async function api(path) {
  try {
    const r = await fetch(path);
    return r.ok ? r.json() : null;
  } catch { return null; }
}

// ── Timer-Logik (identisch zur Android-App) ──────
function elapsedMs(timer) {
  if (!timer) return 0;
  if (timer.isRunning) {
    return Math.max(0, Date.now() - timer.startTime - timer.pausedDuration);
  }
  const froze = timer.pausedAt ?? Date.now();
  return Math.max(0, froze - timer.startTime - timer.pausedDuration);
}

function fmtTime(ms) {
  const s = Math.floor(ms / 1000);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  return `${pad(h)}:${pad(m)}:${pad(sec)}`;
}

function fmtHours(h) {
  if (!h || h <= 0) return '0h';
  const hh = Math.floor(h);
  const mm = Math.round((h - hh) * 60);
  if (mm === 0) return `${hh}h`;
  if (hh === 0) return `${mm}min`;
  return `${hh}h ${mm}min`;
}

function pad(n) { return String(n).padStart(2, '0'); }

function esc(s) {
  if (!s) return '';
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ── Timer rendern ─────────────────────────────────
function renderTimer() {
  const card = document.getElementById('timer-card');
  if (state.timerTick) { clearInterval(state.timerTick); state.timerTick = null; }

  if (!state.timer) { card.classList.add('hidden'); return; }

  const proj = state.projects.find(p => p.id === state.timer.projectId);
  const color = state.timer.projectColor || proj?.color || '#2196F3';

  card.classList.remove('hidden');
  document.getElementById('timer-dot').style.background = color;
  document.getElementById('timer-project-name').textContent =
    state.timer.projectName || proj?.name || 'Unbekanntes Projekt';
  document.getElementById('timer-desc').textContent = state.timer.description || '';

  const badge = document.getElementById('timer-badge');
  const disp  = document.getElementById('timer-display');

  function tick() {
    disp.textContent = fmtTime(elapsedMs(state.timer));
    if (state.timer.isRunning) {
      badge.textContent = 'LÄUFT';  badge.className = 'timer-badge running';
    } else {
      badge.textContent = 'PAUSIERT'; badge.className = 'timer-badge paused';
    }
  }
  tick();
  if (state.timer.isRunning) state.timerTick = setInterval(tick, 1000);
}

// ── Statistik-Kacheln ─────────────────────────────
function renderStats() {
  const now   = new Date();
  const todayStart  = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekDay     = now.getDay() === 0 ? 6 : now.getDay() - 1; // Mo=0
  const weekStart   = new Date(now.getFullYear(), now.getMonth(), now.getDate() - weekDay).getTime();
  const monthStart  = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  const sum = (filter) => state.workLogs.filter(filter).reduce((s,l) => s + l.hoursWorked, 0);

  document.getElementById('stat-today').textContent    = fmtHours(sum(l => l.date >= todayStart));
  document.getElementById('stat-week').textContent     = fmtHours(sum(l => l.date >= weekStart));
  document.getElementById('stat-month').textContent    = fmtHours(sum(l => l.date >= monthStart));
  document.getElementById('stat-projects').textContent =
    state.projects.filter(p => p.status === 'ACTIVE').length;
}

// ── WorkLog-Karte ─────────────────────────────────
function logHtml(log) {
  const color = log.projectColor || '#2196F3';
  const dateStr = new Date(log.date).toLocaleDateString('de-DE',
    { weekday: 'short', day: '2-digit', month: '2-digit', year: '2-digit' });

  const billClass = log.billableStatus === 'BILLED' ? 'badge-billed' : 'badge-unbilled';
  const billTxt   = log.billableStatus === 'BILLED' ? '✓ Abgerechnet' :
                    log.billableStatus === 'PARTIAL' ? '½ Teilweise' : '• Offen';

  return `
  <div class="log-item">
    <div class="log-bar" style="background:${color}"></div>
    <div class="log-info">
      <div class="log-project">${esc(log.projectName || 'Unbekannt')}</div>
      <div class="log-desc ${log.description ? '' : 'no-desc'}">
        ${log.description ? esc(log.description) : 'Keine Beschreibung'}
      </div>
      <div class="log-meta">${dateStr} &nbsp;<span class="${billClass}">${billTxt}</span></div>
    </div>
    <div class="log-hours">
      <div class="log-h-val">${fmtHours(log.hoursWorked)}</div>
    </div>
  </div>`;
}

function renderRecentLogs() {
  const el = document.getElementById('recent-logs');
  const recent = state.workLogs.slice(0, 10);
  if (recent.length === 0) {
    el.innerHTML = `<div class="empty-state">
      <div class="empty-icon">📋</div>
      <div class="empty-title">Noch keine Einträge</div>
      <div class="empty-desc">Starte die Android-App und logge deine erste Arbeitszeit.</div>
    </div>`;
    return;
  }
  el.innerHTML = recent.map(logHtml).join('');
}

function renderDashboard() {
  renderTimer();
  renderStats();
  renderRecentLogs();
}

// ── Projekte ──────────────────────────────────────
function renderProjects() {
  const el = document.getElementById('projects-container');
  if (state.projects.length === 0) {
    el.innerHTML = `<div class="empty-state">
      <div class="empty-icon">📁</div>
      <div class="empty-title">Keine Projekte</div>
      <div class="empty-desc">Erstelle Projekte in der Android-App.<br>Sie werden hier automatisch angezeigt.</div>
    </div>`;
    return;
  }

  const enriched = state.projects.map(p => {
    const logs = state.workLogs.filter(l => l.projectId === p.id);
    return {
      ...p,
      totalHours:  logs.reduce((s,l) => s + l.hoursWorked, 0),
      billedHours: logs.filter(l => l.billableStatus === 'BILLED').reduce((s,l) => s + l.hoursWorked, 0),
      logCount:    logs.length,
    };
  }).sort((a,b) => b.totalHours - a.totalHours);

  el.innerHTML = enriched.map(p => {
    const stCls = { ACTIVE:'active', PAUSED:'paused', ARCHIVED:'archived' }[p.status] ?? 'active';
    const stTxt = { ACTIVE:'Aktiv', PAUSED:'Pausiert', ARCHIVED:'Archiviert' }[p.status] ?? p.status;
    const rev = p.hourlyRate > 0 ? (p.totalHours * p.hourlyRate).toFixed(2) : null;

    return `
    <div class="project-card">
      <div class="project-header">
        <div class="project-avatar" style="background:${p.color}22;color:${p.color}">📁</div>
        <div class="project-meta">
          <div class="project-name">${esc(p.name)}</div>
          ${p.description ? `<div class="project-desc">${esc(p.description)}</div>` : ''}
        </div>
        <span class="status-pill status-${stCls}">${stTxt}</span>
      </div>
      <div class="project-numbers">
        <div class="p-num">
          <div class="p-num-val">${fmtHours(p.totalHours)}</div>
          <div class="p-num-lbl">Gesamt</div>
        </div>
        <div class="p-num">
          <div class="p-num-val">${p.logCount}</div>
          <div class="p-num-lbl">Einträge</div>
        </div>
        ${p.hourlyRate > 0 ? `
        <div class="p-num">
          <div class="p-num-val">${p.hourlyRate}€/h</div>
          <div class="p-num-lbl">Stundensatz</div>
        </div>
        <div class="p-num">
          <div class="p-num-val">${rev}€</div>
          <div class="p-num-lbl">Wert (offen)</div>
        </div>` : ''}
      </div>
    </div>`;
  }).join('');
}

// ── Verlauf ───────────────────────────────────────
async function loadHistory() {
  const el  = document.getElementById('history-list');
  el.innerHTML = '<div class="loading">Lade Verlauf …</div>';

  const pid  = document.getElementById('filter-project').value;
  const from = document.getElementById('filter-from').value;
  const to   = document.getElementById('filter-to').value;

  const params = new URLSearchParams({ limit: 300 });
  if (pid)  params.set('projectId', pid);
  if (from) params.set('from', new Date(from).getTime());
  if (to)   params.set('to',   new Date(to + 'T23:59:59').getTime());

  const logs = await api(`/api/worklogs?${params}`) ?? [];

  if (logs.length === 0) {
    el.innerHTML = `<div class="empty-state">
      <div class="empty-icon">🔍</div>
      <div class="empty-title">Keine Einträge</div>
      <div class="empty-desc">Ändere die Filter oder starte die Zeiterfassung.</div>
    </div>`;
    return;
  }
  el.innerHTML = logs.map(logHtml).join('');
}

// ── Projekt-Filter befüllen ────────────────────────
function fillProjectFilter() {
  const sel = document.getElementById('filter-project');
  const cur = sel.value;
  sel.innerHTML = '<option value="">Alle Projekte</option>';
  state.projects.forEach(p => {
    const opt = document.createElement('option');
    opt.value = p.id; opt.textContent = p.name;
    if (String(p.id) === cur) opt.selected = true;
    sel.appendChild(opt);
  });
}

// ── Uhr ───────────────────────────────────────────
function startClock() {
  const el = document.getElementById('current-time');
  const tick = () => {
    el.textContent = new Date().toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
  };
  tick();
  setInterval(tick, 30_000);
}

// ── Daten laden ───────────────────────────────────
async function loadAllData() {
  const btn = document.querySelector('.refresh-btn');
  btn?.classList.add('spinning');

  const [projects, workLogs, timer] = await Promise.all([
    api('/api/projects'),
    api('/api/worklogs?limit=500'),
    api('/api/timer'),
  ]);

  state.projects = projects ?? [];
  state.workLogs = workLogs ?? [];
  state.timer    = timer;

  fillProjectFilter();
  renderDashboard();
  if (state.page === 'projects') renderProjects();
  if (state.page === 'history')  loadHistory();

  btn?.classList.remove('spinning');
}

function refreshData() { loadAllData(); }

// ── App starten ────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  startClock();
  await loadAllData();

  // Auto-Refresh alle 30 Sek.
  state.autoRefresh = setInterval(loadAllData, 30_000);

  // Filter-Events
  document.getElementById('filter-project').addEventListener('change', loadHistory);
  document.getElementById('filter-from').addEventListener('change', loadHistory);
  document.getElementById('filter-to').addEventListener('change', loadHistory);

  // Standard-Datumsfilter: aktueller Monat
  const now = new Date();
  document.getElementById('filter-from').value =
    new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
  document.getElementById('filter-to').value = now.toISOString().slice(0, 10);

  // Beim Tab-Wechsel zurück → Refresh, wenn >30 s vergangen
  let lastRefresh = Date.now();
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - lastRefresh > 30_000) {
      loadAllData();
      lastRefresh = Date.now();
    }
  });
});

