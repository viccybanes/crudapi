SELECT * FROM tasks;
SELECT * FROM tasks WHERE done = TRUE;
SELECT COUNT(*) FROM tasks;

-- The next two statements change every row. Use a disposable test database.
UPDATE tasks SET done = TRUE;
DELETE FROM tasks WHERE done = TRUE;
