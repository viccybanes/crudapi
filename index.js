const express = require('express');
const app = express();
const port = 3000;
const swaggerUi = require('swagger-ui-express');
const openapiDocument = require('./openapi.json');

// Database 
const Database = require('better-sqlite3');
const db = new Database('tasks.db');

db.exec(`
  CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY,
    title TEXT,
    done INTEGER
  )
`);

const result = db.prepare('SELECT COUNT(*) AS count FROM tasks').get();

if (result.count === 0) {
  const insertTask = db.prepare(
    'INSERT INTO tasks (title, done) VALUES (?, ?)'
  );

  insertTask.run('Task 1', 0);
  insertTask.run('Task 2', 1);
  insertTask.run('Task 3', 1);
}

app.use(express.json());
app.use('/docs', swaggerUi.serve, swaggerUi.setup(openapiDocument));

let tasks = [
  { id: 1, title: 'Task 1', description: 'This is task 1', done: false },
  { id: 2, title: 'Task 2', description: 'This is task 2', done: true },
  { id: 3, title: 'Task 3', description: 'This is task 3', done: true }

]

app.get('/', (req, res) => {
  res.json({ "name": 'TASK API', "version": "1.0", "endpoints": ["/tasks"] });
});

app.get('/tasks', (req, res) => {
  res.json(tasks);
});

app.post('/tasks', (req, res) => {
  if (typeof req.body.title !== 'string' || !req.body.title.trim()) {
    return res.status(400).json({ error: 'Title is required' });
  }

  const task = {
    id: Math.max(0, ...tasks.map(task => task.id)) + 1,
    title: req.body.title,
    done: false
  };

  tasks.push(task);
  res.status(201).json(task);
});

app.put('/tasks/:id', (req, res) => {
  const task = tasks.find(t => t.id === parseInt(req.params.id));

  if (!task) {
    return res.status(404).json({ error: `Task ${req.params.id} not found` });
  }

  const hasTitle = Object.hasOwn(req.body, 'title');
  const hasDone = Object.hasOwn(req.body, 'done');

  if (
    (!hasTitle && !hasDone) ||
    (hasTitle && (typeof req.body.title !== 'string' || !req.body.title.trim())) ||
    (hasDone && typeof req.body.done !== 'boolean')
  ) {
    return res.status(400).json({ error: 'Provide a valid title and/or done value' });
  }

  if (hasTitle) task.title = req.body.title;
  if (hasDone) task.done = req.body.done;

  res.json(task);
});

app.delete('/tasks/:id', (req, res) => {
  const taskIndex = tasks.findIndex(t => t.id === parseInt(req.params.id));

  if (taskIndex === -1) {
    return res.status(404).json({ error: `Task ${req.params.id} not found` });
  }

  tasks.splice(taskIndex, 1);
  res.status(204).send();
});

app.get('/tasks/:id', (req, res) => {
  const task = tasks.find(t => t.id === parseInt(req.params.id));
  if (!task) {
    return res.status(404).json({ error: `Task ${req.params.id} not found` });
  }
  res.json(task);
});


app.get('/health', (req, res) => {
  res.json({ status: 'OK' });
});

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`);
});

