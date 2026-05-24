/* ─────────────────────────────────────────
   TimeLog Web-Dashboard  –  app.js
   Echtzeit-Anzeige für Freddy's iPhone
───────────────────────────────────────── */

/* TimeLog Web-Dashboard
   Firebase Auth + Firestore */

const ROLES = {
  ADMIN: 'ADMIN',
  CUSTOMER: 'CUSTOMER',
  NEW: 'NEW',
};

const state = {
  page: 'dashboard',
  projects: [],
  workLogs: [],
  timer: null,
  timerTick: null,
  autoRefresh: null,
  user: null,
  userProfile: null,
  authMode: 'login',
};

let auth = null;
let db = null;

const usersCol = () => db.collection('users');
const rootRef = () => db.collection('shared').doc('default');

function esc(s) {
  if (!s) return '';
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function pad(n) { return String(n).padStart(2, '0'); }

function roundUpToQuarter(h) {
  if (!h || h <= 0) return 0;
  return Math.ceil(h * 4) / 4;
}

function fmtTime(ms) {
  const s = Math.floor(ms / 1000);
  return `${pad(Math.floor(s / 3600))}:${pad(Math.floor((s % 3600) / 60))}:${pad(s % 60)}`;
}

function fmtHours(h) {
  if (!h || h <= 0) return '0h';
  const hh = Math.floor(h);
  const mm = Math.round((h - hh) * 60);
  if (mm === 0) return `${hh}h`;
  if (hh === 0) return `${mm}min`;
  return `${hh}h ${mm}min`;
}

function fmtCurrency(amount) {
  return new Intl.NumberFormat('de-DE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0);
}

function projectKey(project) {
  return String(project?.cloudId || project?.id || '');
}

function logProjectKey(log) {
  return String(log?.projectCloudId || log?.projectId || '');
}

function getBilledHours(log) {
  const workedHours = Number(log.hoursWorked) || 0;
  const explicitBilledHours = Math.max(0, Number(log.hoursBilled) || 0);

  if (log.billableStatus === 'BILLED') {
    return Math.min(workedHours, explicitBilledHours > 0 ? explicitBilledHours : workedHours);
  }

  return Math.min(workedHours, explicitBilledHours);
}

function elapsedMs(timer) {
  if (!timer) return 0;
  if (timer.isRunning) return Math.max(0, Date.now() - timer.startTime - timer.pausedDuration);
  const froze = timer.pausedAt ?? Date.now();
  return Math.max(0, froze - timer.startTime - timer.pausedDuration);
}

function setAuthError(msg) {
  const el = document.getElementById('auth-error');
  el.textContent = msg || '';
  el.classList.toggle('hidden', !msg);
}

function setAuthMode(mode) {
  state.authMode = mode;
  document.getElementById('login-form').classList.toggle('hidden', mode !== 'login');
  document.getElementById('register-form').classList.toggle('hidden', mode !== 'register');
  document.getElementById('auth-toggle').textContent =
    mode === 'login' ? 'Noch kein Konto? Registrieren' : 'Bereits ein Konto? Login';
  setAuthError('');
}

function showSection(sectionId) {
  document.getElementById('auth-screen').classList.add('hidden');
  document.getElementById('pending-screen').classList.add('hidden');
  document.getElementById('app-shell').classList.add('hidden');
  document.getElementById(sectionId).classList.remove('hidden');
}

function updateHeaderUser() {
  document.getElementById('current-user-email').textContent = state.user?.email || '';
}

function isAdmin() { return state.userProfile?.role === ROLES.ADMIN; }
function isCustomer() { return state.userProfile?.role === ROLES.CUSTOMER; }
function isNewUser() { return state.userProfile?.role === ROLES.NEW; }

function navigate(name, btn) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById(`page-${name}`).classList.add('active');
  state.page = name;

  if (name === 'projects') {
    // Detailansicht schließen wenn zur Projektliste navigiert wird
    document.getElementById('project-detail')?.classList.add('hidden');
    document.getElementById('projects-container')?.classList.remove('hidden');
    renderProjects();
  }
  if (name === 'history') loadHistory();
  if (name === 'dashboard') renderDashboard();
  if (name === 'admin') loadAdminUsers();
}

function renderTimer() {
  const card = document.getElementById('timer-card');
  if (state.timerTick) clearInterval(state.timerTick);
  state.timerTick = null;
  if (!state.timer) return card.classList.add('hidden');

  const proj = state.projects.find(p => projectKey(p) === logProjectKey(state.timer));
  card.classList.remove('hidden');
  document.getElementById('timer-dot').style.background = state.timer.projectColor || proj?.color || '#2196F3';
  document.getElementById('timer-project-name').textContent = state.timer.projectName || proj?.name || 'Unbekannt';
  document.getElementById('timer-desc').textContent = state.timer.description || '';

  const badge = document.getElementById('timer-badge');
  const disp = document.getElementById('timer-display');
  const tick = () => {
    disp.textContent = fmtTime(elapsedMs(state.timer));
    if (state.timer.isRunning) {
      badge.textContent = 'LAEUFT';
      badge.className = 'timer-badge running';
    } else {
      badge.textContent = 'PAUSIERT';
      badge.className = 'timer-badge paused';
    }
  };
  tick();
  if (state.timer.isRunning) state.timerTick = setInterval(tick, 1000);
}

function renderStats() {
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekDay = now.getDay() === 0 ? 6 : now.getDay() - 1;
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - weekDay).getTime();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
  const sum = (f) => state.workLogs.filter(f).reduce((s, l) => s + (l.hoursWorked || 0), 0);

  document.getElementById('stat-today').textContent = fmtHours(sum(l => l.date >= todayStart));
  document.getElementById('stat-week').textContent = fmtHours(sum(l => l.date >= weekStart));
  document.getElementById('stat-month').textContent = fmtHours(sum(l => l.date >= monthStart));
  document.getElementById('stat-projects').textContent = state.projects.filter(p => p.status === 'ACTIVE').length;
}

function logHtml(log) {
  const dateStr = new Date(log.date).toLocaleDateString('de-DE', {
    weekday: 'short', day: '2-digit', month: '2-digit', year: '2-digit'
  });
  const billClass = log.billableStatus === 'BILLED' ? 'badge-billed' : 'badge-unbilled';
  const billTxt = log.billableStatus === 'BILLED' ? 'Abgerechnet' : (log.billableStatus === 'PARTIAL' ? 'Teilweise' : 'Offen');
  const editBtn = isAdmin()
    ? `<button class="log-edit-btn" onclick="openEditLogModal(${log.id})" title="Bearbeiten">✏️</button>`
    : '';

  return `<div class="log-item" data-log-id="${log.id}">
    <div class="log-bar" style="background:${log.projectColor || '#2196F3'}"></div>
    <div class="log-info">
      <div class="log-project">${esc(log.projectName || 'Unbekannt')}</div>
      <div class="log-desc ${log.description ? '' : 'no-desc'}">${log.description ? esc(log.description) : 'Keine Beschreibung'}</div>
      <div class="log-meta">${dateStr} &nbsp;<span class="${billClass}">${billTxt}</span></div>
    </div>
    <div class="log-hours">
      <div class="log-h-val">${fmtHours(log.hoursWorked)}</div>
      ${editBtn}
    </div>
  </div>`;
}

function renderRecentLogs() {
  const el = document.getElementById('recent-logs');
  const recent = state.workLogs.slice(0, 10);
  if (recent.length === 0) {
    el.innerHTML = '<div class="empty-state"><div class="empty-title">Noch keine Eintraege</div></div>';
    return;
  }
  el.innerHTML = recent.map(logHtml).join('');
}

function renderDashboard() {
  renderTimer();
  renderStats();
  renderRecentLogs();
}

function renderProjects() {
  const el = document.getElementById('projects-container');
  if (state.projects.length === 0) {
    el.innerHTML = '<div class="empty-state"><div class="empty-title">Keine Projekte</div></div>';
    return;
  }

  const enriched = state.projects.map(p => {
    const key = projectKey(p);
    const logs = state.workLogs.filter(l => logProjectKey(l) === key);
    const totalHours = logs.reduce((sum, log) => sum + (Number(log.hoursWorked) || 0), 0);
    const outstandingHours = logs.reduce((sum, log) => {
      const workedHours = Number(log.hoursWorked) || 0;
      return sum + Math.max(0, workedHours - getBilledHours(log));
    }, 0);

    return {
      ...p,
      totalHours,
      logCount: logs.length,
      outstandingHours,
      outstandingAmount: outstandingHours * (Number(p.hourlyRate) || 0),
    };
  }).sort((a, b) => b.totalHours - a.totalHours);

  el.innerHTML = enriched.map(p => `<div class="project-card project-card-clickable" onclick="showProjectDetail('${projectKey(p)}')">
    <div class="project-header">
      <div class="project-avatar" style="background:${p.color}22;color:${p.color}">📁</div>
      <div class="project-meta"><div class="project-name">${esc(p.name)}</div></div>
      <span class="status-pill status-${(p.status || 'ACTIVE').toLowerCase()}">${esc(p.status || 'ACTIVE')}</span>
    </div>
    <div class="project-numbers">
      <div class="p-num"><div class="p-num-val">${fmtHours(p.totalHours)}</div><div class="p-num-lbl">Gesamt</div></div>
      <div class="p-num"><div class="p-num-val">${p.logCount}</div><div class="p-num-lbl">Eintraege</div></div>
      <div class="p-num"><div class="p-num-val">${fmtHours(p.outstandingHours)}</div><div class="p-num-lbl">Offene Stunden</div></div>
      <div class="p-num"><div class="p-num-val p-num-val-money">${fmtCurrency(Number(p.hourlyRate) || 0)}</div><div class="p-num-lbl">Stundenlohn</div></div>
      <div class="p-num"><div class="p-num-val p-num-val-money">${fmtCurrency(p.outstandingAmount)}</div><div class="p-num-lbl">Offener Betrag</div></div>
    </div>
    <div class="project-card-hint">Details anzeigen →</div>
  </div>`).join('');
}

function showProjectDetail(projectId) {
  const p = state.projects.find(pr => projectKey(pr) === String(projectId));
  if (!p) return;

  const logs = state.workLogs.filter(l => logProjectKey(l) === String(projectId));
  const totalHours = logs.reduce((s, l) => s + (Number(l.hoursWorked) || 0), 0);
  const billedHours = logs.reduce((s, l) => s + getBilledHours(l), 0);
  const outstandingHours = Math.max(0, totalHours - billedHours);
  const outstandingAmount = outstandingHours * (Number(p.hourlyRate) || 0);

  document.getElementById('detail-title').textContent = p.name;
  document.getElementById('detail-info').innerHTML = `
    <div class="detail-info-top">
      <div class="project-avatar" style="background:${p.color}22;color:${p.color};width:52px;height:52px;border-radius:14px;display:flex;align-items:center;justify-content:center;font-size:24px;">📁</div>
      <div>
        <div style="font-size:18px;font-weight:700;">${esc(p.name)}</div>
        <span class="status-pill status-${(p.status || 'ACTIVE').toLowerCase()}" style="margin-top:4px;display:inline-block;">${esc(p.status || 'ACTIVE')}</span>
      </div>
    </div>
    <div class="project-numbers" style="margin-top:14px;">
      <div class="p-num"><div class="p-num-val">${fmtHours(totalHours)}</div><div class="p-num-lbl">Gesamtstunden</div></div>
      <div class="p-num"><div class="p-num-val">${fmtHours(billedHours)}</div><div class="p-num-lbl">Abgerechnet</div></div>
      <div class="p-num"><div class="p-num-val">${fmtHours(outstandingHours)}</div><div class="p-num-lbl">Offen</div></div>
      <div class="p-num"><div class="p-num-val p-num-val-money">${fmtCurrency(Number(p.hourlyRate) || 0)}</div><div class="p-num-lbl">Stundenlohn</div></div>
      <div class="p-num"><div class="p-num-val p-num-val-money">${fmtCurrency(outstandingAmount)}</div><div class="p-num-lbl">Offener Betrag</div></div>
      <div class="p-num"><div class="p-num-val">${logs.length}</div><div class="p-num-lbl">Einträge</div></div>
    </div>`;

  document.getElementById('detail-logs').innerHTML = logs.length
    ? logs.map(logHtml).join('')
    : '<div class="empty-state"><div class="empty-title">Keine Einträge</div></div>';

  document.getElementById('projects-container').classList.add('hidden');
  const detail = document.getElementById('project-detail');
  detail.classList.remove('hidden');
  detail.scrollTop = 0;
}

function closeProjectDetail() {
  document.getElementById('project-detail').classList.add('hidden');
  document.getElementById('projects-container').classList.remove('hidden');
}

window.showProjectDetail = showProjectDetail;
window.closeProjectDetail = closeProjectDetail;

// ── WorkLog bearbeiten (Admin) ────────────────────────────────
function openEditLogModal(logId) {
  const log = state.workLogs.find(l => String(l.id) === String(logId));
  if (!log) return;

  document.getElementById('edit-log-id').value = String(logId);

  // Projekt-Dropdown befüllen
  const sel = document.getElementById('edit-log-project');
  sel.innerHTML = state.projects.map(p =>
    `<option value="${projectKey(p)}" ${projectKey(p) === logProjectKey(log) ? 'selected' : ''}>${esc(p.name)}</option>`
  ).join('');

  // Datum (ISO-Format YYYY-MM-DD für date input)
  document.getElementById('edit-log-date').value = new Date(log.date).toISOString().slice(0, 10);

  // Stunden (realer Wert, nicht aufgerundeter)
  document.getElementById('edit-log-hours').value = log.hoursWorked;

  document.getElementById('edit-log-desc').value = log.description || '';
  document.getElementById('edit-log-status').value = log.billableStatus || 'UNBILLED';

  document.getElementById('edit-modal-error').classList.add('hidden');
  document.getElementById('edit-modal-overlay').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeEditLogModal() {
  document.getElementById('edit-modal-overlay').classList.add('hidden');
  document.body.style.overflow = '';
}

async function saveEditLog() {
  const logId = document.getElementById('edit-log-id').value;
  const selectedProjectKey = String(document.getElementById('edit-log-project').value || '');
  const dateVal = document.getElementById('edit-log-date').value;
  const hoursVal = document.getElementById('edit-log-hours').value.replace(',', '.');
  const description = document.getElementById('edit-log-desc').value.trim();
  const billableStatus = document.getElementById('edit-log-status').value;
  const errEl = document.getElementById('edit-modal-error');

  const hours = parseFloat(hoursVal);
  if (!dateVal || !Number.isFinite(hours) || hours <= 0) {
    errEl.textContent = 'Bitte gültiges Datum und Stunden angeben.';
    errEl.classList.remove('hidden');
    return;
  }

  const dateTs = new Date(dateVal).getTime();
  const project = state.projects.find(p => projectKey(p) === selectedProjectKey);
  if (!project) {
    errEl.textContent = 'Projekt nicht gefunden.';
    errEl.classList.remove('hidden');
    return;
  }

  try {
    errEl.classList.add('hidden');
    const saveBtn = document.querySelector('#edit-modal-overlay .modal-btn-primary');
    saveBtn.disabled = true;
    saveBtn.textContent = 'Speichern …';

    await rootRef().collection('workLogs').doc(logId).update({
      projectId: Number(project.id) || 0,
      projectCloudId: projectKey(project),
      projectName: project?.name || '',
      projectColor: project?.color || '#2196F3',
      date: dateTs,
      hoursWorked: hours,
      description,
      billableStatus,
      updatedAt: Date.now(),
    });

    closeEditLogModal();
    await loadAllData();
  } catch (e) {
    errEl.textContent = 'Fehler beim Speichern: ' + (e?.message || 'Unbekannter Fehler');
    errEl.classList.remove('hidden');
  } finally {
    const saveBtn = document.querySelector('#edit-modal-overlay .modal-btn-primary');
    if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = 'Speichern'; }
  }
}

window.openEditLogModal = openEditLogModal;
window.closeEditLogModal = closeEditLogModal;
window.saveEditLog = saveEditLog;

function fillProjectFilter() {
  const sel = document.getElementById('filter-project');
  const cur = sel.value;
  sel.innerHTML = '<option value="">Alle Projekte</option>';
  state.projects.forEach(p => {
    const opt = document.createElement('option');
    opt.value = projectKey(p);
    opt.textContent = p.name;
    if (String(cur) === projectKey(p)) opt.selected = true;
    sel.appendChild(opt);
  });
}

function loadHistory() {
  const el = document.getElementById('history-list');
  const pid = document.getElementById('filter-project').value;
  const from = document.getElementById('filter-from').value;
  const to = document.getElementById('filter-to').value;
  const fromTs = from ? new Date(from).getTime() : null;
  const toTs = to ? new Date(`${to}T23:59:59`).getTime() : null;

  const filtered = state.workLogs.filter(l => {
    if (pid && logProjectKey(l) !== String(pid)) return false;
    if (fromTs && l.date < fromTs) return false;
    if (toTs && l.date > toTs) return false;
    return true;
  });

  el.innerHTML = filtered.length ? filtered.slice(0, 300).map(logHtml).join('') :
    '<div class="empty-state"><div class="empty-title">Keine Eintraege</div></div>';
}

function startClock() {
  const el = document.getElementById('current-time');
  const tick = () => { el.textContent = new Date().toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' }); };
  tick();
  setInterval(tick, 30_000);
}

function normalizeAllowedProjectIds(rawList) {
  return (rawList || []).map(v => String(v || '').trim()).filter(Boolean);
}

async function ensureUserProfile(user) {
  const ref = usersCol().doc(user.uid);
  const snap = await ref.get();
  if (!snap.exists) {
    const profile = {
      uid: user.uid,
      email: user.email || '',
      role: ROLES.NEW,
      allowedProjectIds: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    await ref.set(profile);
    return profile;
  }
  return snap.data();
}

async function loadAllProjectsAdmin() {
  const snap = await rootRef().collection('projects').get();
  return snap.docs.map(d => ({ ...d.data(), cloudId: d.id }));
}

async function loadProjectsForCustomer(allowedIds) {
  if (!allowedIds.length) return [];
  const chunks = [];
  for (let i = 0; i < allowedIds.length; i += 10) chunks.push(allowedIds.slice(i, i + 10));

  const results = [];
  for (const ids of chunks) {
    const snap = await rootRef().collection('projects')
      .where(firebase.firestore.FieldPath.documentId(), 'in', ids)
      .get();
    snap.docs.forEach(d => results.push({ ...d.data(), cloudId: d.id }));
  }
  return results;
}

async function loadWorkLogsForAllowed(allowedIds) {
  if (!allowedIds.length) return [];
  const chunks = [];
  for (let i = 0; i < allowedIds.length; i += 10) chunks.push(allowedIds.slice(i, i + 10));

  const results = [];
  for (const ids of chunks) {
    // Kein orderBy hier – würde Composite Index erfordern.
    // Sortierung erfolgt client-seitig unten.
    const snap = await rootRef().collection('workLogs')
      .where('projectCloudId', 'in', ids)
      .limit(300)
      .get();
    snap.docs.forEach(d => results.push(d.data()));
  }
  return results.sort((a, b) => (b.date || 0) - (a.date || 0));
}

async function loadAllData() {
  if (!state.user || !state.userProfile || !db) return;
  if (isNewUser()) return;

  const btn = document.querySelector('.refresh-btn');
  btn?.classList.add('spinning');

  try {
    const allowedIds = normalizeAllowedProjectIds(state.userProfile.allowedProjectIds);

    let projects = [];
    let workLogs = [];

    if (isAdmin()) {
      const [pSnap, wSnap] = await Promise.all([
        rootRef().collection('projects').get(),
        rootRef().collection('workLogs').orderBy('date', 'desc').limit(500).get(),
      ]);
      projects = pSnap.docs.map(d => ({ ...d.data(), cloudId: d.id }));
      workLogs = wSnap.docs.map(d => d.data());
    } else if (isCustomer()) {
      // Separat laden: wenn WorkLogs fehlschlagen, erscheinen Projekte trotzdem.
      try {
        projects = await loadProjectsForCustomer(allowedIds);
      } catch (e) {
        console.error('Projekte laden fehlgeschlagen', e);
      }
      try {
        workLogs = await loadWorkLogsForAllowed(allowedIds);
      } catch (e) {
        console.error('WorkLogs laden fehlgeschlagen', e);
      }
    }

    let timerSnap = null;
    try {
      timerSnap = await rootRef().collection('meta').doc('activeTimer').get();
    } catch (e) {
      console.warn('activeTimer lesen fehlgeschlagen (ignoriert)', e);
    }
    const projectsByKey = new Map(projects.map(p => [projectKey(p), p]));

    state.projects = projects.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
    state.workLogs = workLogs.map(log => ({
      ...log,
      hoursWorked: roundUpToQuarter(Number(log.hoursWorked) || 0),
      projectName: log.projectName || projectsByKey.get(logProjectKey(log))?.name,
      projectColor: log.projectColor || projectsByKey.get(logProjectKey(log))?.color,
    }));

    const timer = timerSnap?.exists ? timerSnap.data() : null;
    state.timer = timer && (isAdmin() || allowedIds.includes(logProjectKey(timer))) ? timer : null;

    fillProjectFilter();
    renderDashboard();
    if (state.page === 'projects') renderProjects();
    if (state.page === 'history') loadHistory();
  } catch (e) {
    console.error('Daten laden fehlgeschlagen', e);
  } finally {
    btn?.classList.remove('spinning');
  }
}

async function loadAdminUsers() {
  if (!isAdmin()) return;
  const el = document.getElementById('admin-users');
  el.innerHTML = '<div class="loading">Lade Nutzer ...</div>';

  const [usersSnap, projects] = await Promise.all([
    usersCol().orderBy('email').get(),
    loadAllProjectsAdmin(),
  ]);

  el.innerHTML = usersSnap.docs.map(doc => {
    const u = doc.data();
    const allowedSet = new Set(normalizeAllowedProjectIds(u.allowedProjectIds));
    const projectChecks = projects.length
      ? projects.map(p => {
          const key = projectKey(p);
          const checked = allowedSet.has(key) ? 'checked' : '';
          return `<label class="admin-project-item"><input type="checkbox" data-project-check value="${esc(key)}" ${checked}> <span>${esc(p.name)} <small class="admin-project-key">(${esc(key)})</small></span></label>`;
        }).join('')
      : '<div class="log-meta">Keine Projekte vorhanden</div>';
    return `<div class="admin-user-card" data-uid="${u.uid}">
      <div class="admin-user-head">
        <div class="admin-user-email">${esc(u.email || u.uid)}</div>
        <select class="admin-role-select" data-role>
          <option value="NEW" ${u.role === 'NEW' ? 'selected' : ''}>NEW</option>
          <option value="CUSTOMER" ${u.role === 'CUSTOMER' ? 'selected' : ''}>CUSTOMER</option>
          <option value="TECHNICIAN" ${u.role === 'TECHNICIAN' ? 'selected' : ''}>TECHNICIAN</option>
          <option value="ADMIN" ${u.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
        </select>
      </div>
      <div class="admin-projects-grid" data-projects-grid>${projectChecks}</div>
      <button class="admin-save-btn" data-save>Speichern</button>
      <div class="log-meta">Projektfreigaben gelten für CUSTOMER (UUID/cloudId-basiert).</div>
    </div>`;
  }).join('');

  el.querySelectorAll('[data-save]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const card = btn.closest('.admin-user-card');
      const uid = card.getAttribute('data-uid');
      const role = card.querySelector('[data-role]').value;
      const allowedProjectIds = Array.from(card.querySelectorAll('[data-project-check]:checked')).map(el => el.value);
      await usersCol().doc(uid).set({ role, allowedProjectIds, updatedAt: Date.now() }, { merge: true });
      await loadAdminUsers();
    });
  });
}

function refreshData() { loadAllData(); }
window.refreshData = refreshData;
window.navigate = navigate;

function clearAppState() {
  state.projects = [];
  state.workLogs = [];
  state.timer = null;
  if (state.timerTick) clearInterval(state.timerTick);
  if (state.autoRefresh) clearInterval(state.autoRefresh);
  state.timerTick = null;
  state.autoRefresh = null;
}

function bindAuthUi() {
  document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    setAuthError('');
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    try {
      await auth.signInWithEmailAndPassword(email, password);
    } catch (err) {
      setAuthError(err?.message || 'Login fehlgeschlagen.');
    }
  });

  document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    setAuthError('');
    const email = document.getElementById('register-email').value.trim();
    const password = document.getElementById('register-password').value;
    const repeat = document.getElementById('register-password-repeat').value;

    if (password !== repeat) {
      setAuthError('Passwoerter stimmen nicht ueberein.');
      return;
    }

    try {
      await auth.createUserWithEmailAndPassword(email, password);
      setAuthMode('login');
    } catch (err) {
      setAuthError(err?.message || 'Registrierung fehlgeschlagen.');
    }
  });

  document.getElementById('auth-toggle').addEventListener('click', () => {
    setAuthMode(state.authMode === 'login' ? 'register' : 'login');
  });

  document.getElementById('forgot-password-btn').addEventListener('click', async () => {
    setAuthError('');
    const email = document.getElementById('login-email').value.trim();
    if (!email) {
      setAuthError('Bitte zuerst die E-Mail-Adresse eingeben.');
      return;
    }
    try {
      await auth.sendPasswordResetEmail(email);
      setAuthError('✅ Reset-E-Mail wurde gesendet. Bitte prüfe dein Postfach.');
    } catch (err) {
      setAuthError(err?.message || 'Fehler beim Senden der Reset-E-Mail.');
    }
  });

  document.getElementById('logout-btn').addEventListener('click', async () => auth.signOut());
  document.getElementById('pending-logout-btn').addEventListener('click', async () => auth.signOut());
}

function bindFilters() {
  document.getElementById('filter-project').addEventListener('change', loadHistory);
  document.getElementById('filter-from').addEventListener('change', loadHistory);
  document.getElementById('filter-to').addEventListener('change', loadHistory);

  const now = new Date();
  document.getElementById('filter-from').value = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
  document.getElementById('filter-to').value = now.toISOString().slice(0, 10);
}

function applyRoleUi() {
  const navAdmin = document.getElementById('nav-admin');
  navAdmin.classList.toggle('hidden', !isAdmin());
}

async function initFirebaseAndAuth() {
  if (!window.firebase) throw new Error('Firebase SDK nicht geladen.');

  const app = firebase.app();
  auth = firebase.auth(app);
  db = firebase.firestore(app);

  auth.onAuthStateChanged(async (user) => {
    state.user = user;
    state.userProfile = null;
    updateHeaderUser();

    if (!user) {
      clearAppState();
      applyRoleUi();
      showSection('auth-screen');
      return;
    }

    state.userProfile = await ensureUserProfile(user);
    applyRoleUi();

    if (isNewUser()) {
      clearAppState();
      showSection('pending-screen');
      return;
    }

    showSection('app-shell');
    await loadAllData();

    if (state.autoRefresh) clearInterval(state.autoRefresh);
    state.autoRefresh = setInterval(loadAllData, 30_000);
  });
}

document.addEventListener('DOMContentLoaded', async () => {
  startClock();
  bindAuthUi();
  bindFilters();
  setAuthMode('login');

  try {
    await initFirebaseAndAuth();
  } catch (e) {
    setAuthError(e.message || 'Firebase Initialisierung fehlgeschlagen.');
    showSection('auth-screen');
  }

  let lastRefresh = Date.now();
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - lastRefresh > 30_000) {
      loadAllData();
      lastRefresh = Date.now();
    }
  });
});

