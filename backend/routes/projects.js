const express     = require('express');
const router      = express.Router();
const { db, save } = require('../database');

// GET – alle Projekte
router.get('/', (_req, res) => {
  const sorted = [...db.projects].sort((a, b) => a.name.localeCompare(b.name));
  res.json(sorted);
});

// POST /sync – Upsert eines Projekts von Android
router.post('/sync', (req, res) => {
  const { id, name, description, color, hourlyRate, status, createdAt, updatedAt } = req.body;
  if (!id || !name) return res.status(400).json({ error: 'id und name sind Pflicht' });

  const idx = db.projects.findIndex(p => p.id === id);
  const project = { id, name, description: description ?? null, color: color ?? '#2196F3',
                    hourlyRate: hourlyRate ?? 0, status: status ?? 'ACTIVE', createdAt, updatedAt };

  if (idx >= 0) db.projects[idx] = project;
  else          db.projects.push(project);

  save();
  res.json({ success: true });
});

// DELETE – Projekt löschen
router.delete('/:id', (req, res) => {
  const id = Number(req.params.id);
  db.projects = db.projects.filter(p => p.id !== id);
  db.work_logs = db.work_logs.filter(w => w.projectId !== id);
  save();
  res.json({ success: true });
});

module.exports = router;
