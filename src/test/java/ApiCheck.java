import java.nio.file.Files;
import java.sql.DriverManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonParser;

public class ApiCheck {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static String base;
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
            try (var api = new TaskApi(database, 0)) {
                base = "http://localhost:" + api.port();
                var response = request("GET", "/tasks", null, 200);
                check(JsonParser.parseString(response).getAsJsonArray().size() == 3, "Missing seeds");
                request("GET", "/tasks/1", null, 200);
                request("GET", "/tasks/999", null, 404);
                request("GET", "/tasks/1%20OR%201=1", null, 404);
            }
            System.out.println("PASS: database setup, three restarts, SQL reads and 404s");
        } finally {
            Files.deleteIfExists(database);
            Files.delete(directory);
        }
    }

    static String request(String method, String path, String body, int expected) throws Exception {
        var publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        var request = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json").method(method, publisher).build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        check(response.statusCode() == expected, method + " " + path + ": " + response.statusCode());
        if (expected >= 400) check(JsonParser.parseString(response.body()).getAsJsonObject().has("error"), "Missing JSON error");
        return response.body();
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
