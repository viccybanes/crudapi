import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

public class TaskApi implements AutoCloseable {
    private static final Gson JSON = new Gson();
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
