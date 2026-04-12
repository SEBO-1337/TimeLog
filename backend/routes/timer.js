const express      = require('express');
const router       = express.Router();
const { db, save } = require('../database');

// GET – aktiver Timer (mit Projekt-Infos angereichert)
router.get('/', (_req, res) => {
  if (!db.active_timer) return res.json(null);
  const proj = db.projects.find(p => p.id === db.active_timer.projectId);
  res.json({
    ...db.active_timer,
    projectName:  proj?.name  ?? null,
    projectColor: proj?.color ?? '#2196F3',
  });
});

// POST /sync – Timer-Status von Android speichern
router.post('/sync', (req, res) => {
  const { projectId, isRunning, startTime, pausedDuration, pausedAt, description } = req.body;
  db.active_timer = {
    projectId, isRunning: !!isRunning,
    startTime, pausedDuration: pausedDuration ?? 0,
    pausedAt: pausedAt ?? null, description: description ?? null,
    updatedAt: Date.now(),
  };
  save();
  res.json({ success: true });
});

// DELETE – Timer gestoppt / verworfen
router.delete('/', (_req, res) => {
  db.active_timer = null;
  save();
  res.json({ success: true });
});

module.exports = router;
