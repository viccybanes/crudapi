SELECT * FROM tasks;
SELECT * FROM tasks WHERE done = 1;
SELECT COUNT(*) FROM tasks;

-- The next two statements change every row. Use a disposable test database.
UPDATE tasks SET done = 1;
DELETE FROM tasks WHERE done = 1;
