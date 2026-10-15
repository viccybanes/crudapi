import java.nio.file.Files;
import java.sql.DriverManager;

public class ApiCheck {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("task-api-check-");
        var database = directory.resolve("tasks.db");
        try {
            for (int restart = 0; restart < 3; restart++) {
                try (var store = new TaskStore(database);
                     var db = DriverManager.getConnection("jdbc:sqlite:" + database);
                     var query = db.prepareStatement("SELECT COUNT(*) FROM tasks");
                     var rows = query.executeQuery()) {
                    check(rows.next() && rows.getInt(1) == 3, "Seeds duplicated");
                }
            }
            System.out.println("PASS: database creation and three restarts");
        } finally {
            Files.deleteIfExists(database);
            Files.delete(directory);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
