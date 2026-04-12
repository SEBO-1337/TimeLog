const express      = require('express');
const router       = express.Router();
const { db, save } = require('../database');

// Hilfsfunktion: Projekt-Infos an einen WorkLog anhängen
function enrich(log) {
  const proj = db.projects.find(p => p.id === log.projectId);
  return { ...log, projectName: proj?.name ?? null, projectColor: proj?.color ?? '#2196F3' };
}

// GET /stats/summary  ← MUSS vor /:id stehen!
router.get('/stats/summary', (_req, res) => {
  const totalHours  = db.work_logs.reduce((s, l) => s + (l.hoursWorked ?? 0), 0);
  const billedHours = db.work_logs.filter(l => l.billableStatus === 'BILLED')
                                   .reduce((s, l) => s + (l.hoursWorked ?? 0), 0);
  const activeProjects = db.projects.filter(p => p.status === 'ACTIVE').length;
  res.json({ totalHours, billedHours, activeProjects, totalLogs: db.work_logs.length });
});

// GET / – WorkLogs gefiltert + angereichert
router.get('/', (req, res) => {
  const { projectId, from, to, limit = 500 } = req.query;
  let logs = [...db.work_logs];

  if (projectId) logs = logs.filter(l => l.projectId === Number(projectId));
  if (from)      logs = logs.filter(l => l.date >= Number(from));
  if (to)        logs = logs.filter(l => l.date <= Number(to));

  logs.sort((a, b) => b.date - a.date);
  res.json(logs.slice(0, Number(limit)).map(enrich));
});

// POST /sync – Upsert eines WorkLogs von Android
router.post('/sync', (req, res) => {
  const {
    id, projectId, description, hoursWorked, hoursBilled,
    date, startTime, endTime, billableStatus, notes, tags, createdAt, updatedAt
  } = req.body;

  if (!id || !projectId || hoursWorked == null) {
    return res.status(400).json({ error: 'id, projectId und hoursWorked sind Pflicht' });
  }

  const log = {
    id, projectId, description: description ?? '',
    hoursWorked, hoursBilled: hoursBilled ?? 0,
    date, startTime: startTime ?? null, endTime: endTime ?? null,
    billableStatus: billableStatus ?? 'UNBILLED',
    notes: notes ?? null, tags: tags ?? null, createdAt, updatedAt
  };

  const idx = db.work_logs.findIndex(l => l.id === id);
  if (idx >= 0) db.work_logs[idx] = log;
  else          db.work_logs.push(log);

  save();
  res.json({ success: true });
});

// DELETE – WorkLog löschen
router.delete('/:id', (req, res) => {
  const id = Number(req.params.id);
  db.work_logs = db.work_logs.filter(l => l.id !== id);
  save();
  res.json({ success: true });
});

module.exports = router;
