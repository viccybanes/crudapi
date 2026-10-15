import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskStore implements AutoCloseable {
    public record Task(long id, String title, String description, boolean done) {}
    private final Connection db;

    public TaskStore(Path file) throws SQLException {
        db = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (var statement = db.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS tasks "
                    + "(id INTEGER PRIMARY KEY, title TEXT, done INTEGER, description TEXT)");
        }
        // Keep A1's optional seed descriptions when opening the earlier three-column database.
        boolean hasDescription = false;
        try (var statement = db.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(tasks)")) {
            while (columns.next()) {
                if (columns.getString("name").equals("description")) hasDescription = true;
            }
        }
        if (!hasDescription) {
            try (var statement = db.createStatement()) {
                statement.executeUpdate("ALTER TABLE tasks ADD COLUMN description TEXT");
            }
            try (var update = db.prepareStatement("UPDATE tasks SET description = ? WHERE id = ? AND title = ?")) {
                for (int i = 1; i <= 3; i++) {
                    update.setString(1, "This is task " + i);
                    update.setInt(2, i);
                    update.setString(3, "Task " + i);
                    update.executeUpdate();
                }
            }
        }
        try (var statement = db.prepareStatement("SELECT COUNT(*) FROM tasks");
             var rows = statement.executeQuery()) {
            if (rows.next() && rows.getInt(1) == 0) {
                try (var insert = db.prepareStatement(
                        "INSERT INTO tasks (title, done, description) VALUES (?, ?, ?)")) {
                    for (int i = 1; i <= 3; i++) {
                        insert.setString(1, "Task " + i);
                        insert.setInt(2, i == 1 ? 0 : 1);
                        insert.setString(3, "This is task " + i);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    public List<Task> all() throws SQLException {
        var tasks = new ArrayList<Task>();
        try (var query = db.prepareStatement("SELECT * FROM tasks");
             var rows = query.executeQuery()) {
            while (rows.next()) {
                tasks.add(new Task(rows.getLong("id"), rows.getString("title"), rows.getString("description"), rows.getInt("done") != 0));
            }
        }
        return tasks;
    }

    public Task find(long id) throws SQLException {
        try (var query = db.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            query.setLong(1, id);
            try (var rows = query.executeQuery()) {
                return rows.next()
                        ? new Task(rows.getLong("id"), rows.getString("title"), rows.getString("description"), rows.getInt("done") != 0)
                        : null;
            }
        }
    }

    public Task create(String title) throws SQLException {
        try (var insert = db.prepareStatement("INSERT INTO tasks (title, done) VALUES (?, ?)")) {
            insert.setString(1, title);
            insert.setInt(2, 0);
            insert.executeUpdate();
        }
        try (var query = db.prepareStatement("SELECT last_insert_rowid()");
             var rows = query.executeQuery()) {
            rows.next();
            return find(rows.getLong(1));
        }
    }

    public Task update(long id, String title, boolean done) throws SQLException {
        try (var update = db.prepareStatement("UPDATE tasks SET title = ?, done = ? WHERE id = ?")) {
            update.setString(1, title);
            update.setInt(2, done ? 1 : 0);
            update.setLong(3, id);
            update.executeUpdate();
        }
        return find(id);
    }

    public void delete(long id) throws SQLException {
        try (var delete = db.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            delete.setLong(1, id);
            delete.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        db.close();
    }
}
