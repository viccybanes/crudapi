import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskStore implements AutoCloseable {
    public record Task(long id, String title, String description, boolean done) {}
    private final Connection db;

    public static String databaseUrl() {
        String url = System.getenv("DATABASE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DATABASE_URL is required; start with docker compose up");
        }
        return url;
    }

    public TaskStore(String url) throws SQLException {
        db = DriverManager.getConnection(url);
        try (var statement = db.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS tasks "
                    + "(id SERIAL PRIMARY KEY, title TEXT, done BOOLEAN, description TEXT)");
        }
        try (var statement = db.prepareStatement("SELECT COUNT(*) FROM tasks");
             var rows = statement.executeQuery()) {
            if (rows.next() && rows.getInt(1) == 0) {
                try (var insert = db.prepareStatement(
                        "INSERT INTO tasks (title, done, description) VALUES (?, ?, ?)")) {
                    for (int i = 1; i <= 3; i++) {
                        insert.setString(1, "Task " + i);
                        insert.setBoolean(2, i != 1);
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
                tasks.add(new Task(rows.getLong("id"), rows.getString("title"), rows.getString("description"), rows.getBoolean("done")));
            }
        }
        return tasks;
    }

    public Task find(long id) throws SQLException {
        try (var query = db.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            query.setLong(1, id);
            try (var rows = query.executeQuery()) {
                return rows.next()
                        ? new Task(rows.getLong("id"), rows.getString("title"), rows.getString("description"), rows.getBoolean("done"))
                        : null;
            }
        }
    }

    public Task create(String title) throws SQLException {
        try (var insert = db.prepareStatement("INSERT INTO tasks (title, done) VALUES (?, ?) RETURNING *")) {
            insert.setString(1, title);
            insert.setBoolean(2, false);
            try (var rows = insert.executeQuery()) {
                rows.next();
                return new Task(rows.getLong("id"), rows.getString("title"), rows.getString("description"), rows.getBoolean("done"));
            }
        }
    }

    public Task update(long id, String title, boolean done) throws SQLException {
        try (var update = db.prepareStatement("UPDATE tasks SET title = ?, done = ? WHERE id = ?")) {
            update.setString(1, title);
            update.setBoolean(2, done);
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
