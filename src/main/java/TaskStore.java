import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TaskStore implements AutoCloseable {
    private final Connection db;

    public TaskStore(Path file) throws SQLException {
        db = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (var statement = db.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS tasks "
                    + "(id INTEGER PRIMARY KEY, title TEXT, done INTEGER)");
        }
        try (var statement = db.prepareStatement("SELECT COUNT(*) FROM tasks");
             var rows = statement.executeQuery()) {
            if (rows.next() && rows.getInt(1) == 0) {
                try (var insert = db.prepareStatement(
                        "INSERT INTO tasks (title, done) VALUES (?, ?)")) {
                    for (int i = 1; i <= 3; i++) {
                        insert.setString(1, "Task " + i);
                        insert.setInt(2, i == 1 ? 0 : 1);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public void close() throws SQLException {
        db.close();
    }
}
