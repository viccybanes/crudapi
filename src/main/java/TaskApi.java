import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public class TaskApi implements AutoCloseable {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final TaskStore store;
    private final HttpServer server;

    public TaskApi(Path database, int port) throws SQLException, IOException {
        store = new TaskStore(database);
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                route(exchange);
            } catch (JsonParseException | IllegalArgumentException error) {
                send(exchange, 400, Map.of("error", "Invalid JSON body"));
            } catch (SQLException error) {
                error.printStackTrace();
                send(exchange, 500, Map.of("error", "Database operation failed"));
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException, SQLException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (method.equals("GET") && path.equals("/")) {
            send(exchange, 200, Map.of("name", "TASK API", "version", "1.0", "endpoints", new String[]{"/tasks"}));
        } else if (method.equals("GET") && path.equals("/health")) {
            send(exchange, 200, Map.of("status", "OK"));
        } else if (method.equals("GET") && path.equals("/tasks")) {
            send(exchange, 200, store.all());
        } else if (method.equals("POST") && path.equals("/tasks")) {
            var body = body(exchange);
            if (!validTitle(body.get("title"))) {
                send(exchange, 400, Map.of("error", "Title is required"));
            } else {
                send(exchange, 201, store.create(body.get("title").getAsString()));
            }
        } else if (path.matches("/tasks/[^/]+")) {
            long id;
            try {
                id = Long.parseLong(path.substring("/tasks/".length()));
            } catch (NumberFormatException error) {
                send(exchange, 404, Map.of("error", "Task not found"));
                return;
            }
            var task = store.find(id);
            if (task == null) {
                send(exchange, 404, Map.of("error", "Task not found"));
            } else if (method.equals("GET")) {
                send(exchange, 200, task);
            } else {
                send(exchange, 405, Map.of("error", "Method not allowed"));
            }
        } else {
            send(exchange, 404, Map.of("error", "Not found"));
        }
    }

    private static JsonObject body(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new IllegalArgumentException("Expected application/json");
        }
        var value = JSON.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), JsonElement.class);
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("Expected an object");
        return value.getAsJsonObject();
    }

    private static boolean validTitle(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && !value.getAsString().isBlank();
    }

    private static void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = JSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close() throws SQLException {
        server.stop(0);
        store.close();
    }

    public static void main(String[] args) throws Exception {
        var api = new TaskApi(Path.of("tasks.db"), 3000);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                api.close();
            } catch (SQLException error) {
                error.printStackTrace();
            }
        }));
        System.out.println("Task API listening on http://localhost:" + api.port());
    }
}
