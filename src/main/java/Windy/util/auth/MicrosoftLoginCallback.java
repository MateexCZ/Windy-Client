package Windy.util.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

final class MicrosoftLoginCallback implements AutoCloseable {
    private enum Stage { WAITING, SIGNING_IN, SUCCESS, FAILED, CLOSED }
    private final HttpServer server;
    private final ExecutorService requests;
    private final ScheduledExecutorService cleanup;
    private final CompletableFuture<String> code = new CompletableFuture<>();
    private final String state;
    private final URI successPage;
    private volatile Stage stage = Stage.WAITING;

    MicrosoftLoginCallback(int port, URI successPage) throws IOException {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        state = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        this.successPage = safeSuccessPage(successPage);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        requests = Executors.newSingleThreadExecutor(task -> daemon(task, "Windy login callback"));
        cleanup = Executors.newSingleThreadScheduledExecutor(task -> daemon(task, "Windy login cleanup"));
        server.setExecutor(requests);
        server.createContext("/callback", this::callback);
        server.createContext("/status", this::status);
        server.start();
    }
    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
    String state() { return state; }
    int port() { return server.getAddress().getPort(); }
    String awaitCode(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return code.get(timeout, unit);
    }
    private void callback(HttpExchange exchange) throws IOException {
        if (!validRequest(exchange, "/callback")) return;
        Map<String, String> parameters;
        try { parameters = query(exchange.getRequestURI().getRawQuery()); }
        catch (IllegalArgumentException error) { page(exchange, 400, "Invalid sign-in response", "Please return to Windy and try again.", false); return; }
        String received = parameters.get("state");
        if (received == null || !MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8))) {
            page(exchange, 400, "Invalid sign-in response", "This response does not belong to the current sign-in attempt.", false);
            return;
        }
        synchronized (this) {
            if (stage != Stage.WAITING) { page(exchange, 410, "Sign-in response already received", "Please return to Windy.", false); return; }
            if (parameters.containsKey("error")) {
                stage = Stage.FAILED;
                code.completeExceptionally(new IOException("Microsoft sign-in was declined or failed."));
                page(exchange, 400, "Sign-in was not completed", "Return to Windy to try again.", false);
                stopLater();
                return;
            }
            String value = parameters.get("code");
            if (value == null || value.isBlank()) { page(exchange, 400, "Missing sign-in code", "Please return to Windy and try again.", false); return; }
            stage = Stage.SIGNING_IN;
            code.complete(value);
        }
        redirect(exchange, "/status");
    }
    private void status(HttpExchange exchange) throws IOException {
        if (!validRequest(exchange, "/status")) return;
        Stage current = stage;
        if (current == Stage.SUCCESS && successPage != null) {
            redirect(exchange, successPage.toASCIIString());
        } else if (current == Stage.SUCCESS) {
            page(exchange, 200, "Authentication successful", "You are signed in. You can return to Windy.", false);
        } else if (current == Stage.FAILED || current == Stage.CLOSED) {
            page(exchange, 200, "Sign-in was not completed", "Return to Windy to try again.", false);
        } else {
            page(exchange, 200, "Finishing sign-in", "Windy is verifying your Minecraft account. Please wait.", true);
        }
    }
    private boolean validRequest(HttpExchange exchange, String path) throws IOException {
        if (!exchange.getRemoteAddress().getAddress().isLoopbackAddress() || !path.equals(exchange.getRequestURI().getPath())) {
            page(exchange, 404, "Page not found", "Return to Windy.", false); return false;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            page(exchange, 405, "Request not supported", "Return to Windy.", false); return false;
        }
        return true;
    }
    static Map<String, String> query(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.length() > 16384) return values;
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) throw new IllegalArgumentException("Invalid query");
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            if (values.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Duplicate query parameter");
        }
        return values;
    }
    static URI safeSuccessPage(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null ? uri : null;
    }
    synchronized void successful() {
        if (stage != Stage.SIGNING_IN) return;
        stage = Stage.SUCCESS;
        stopLater();
    }
    synchronized void failed() {
        if (stage == Stage.SUCCESS || stage == Stage.CLOSED) return;
        stage = Stage.FAILED;
        code.completeExceptionally(new IOException("Microsoft sign-in failed."));
        stopLater();
    }
    synchronized void cancelIfPending() { if (stage != Stage.SUCCESS) close(); }
    private void stopLater() { if (!cleanup.isShutdown()) cleanup.schedule(this::close, 60, TimeUnit.SECONDS); }
    private static void headers(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'");
    }
    private static void redirect(HttpExchange exchange, String location) throws IOException {
        headers(exchange);
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }
    private static void page(HttpExchange exchange, int status, String title, String message, boolean refresh) throws IOException {
        headers(exchange);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        String html = "<!doctype html><html lang='en'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + (refresh ? "<meta http-equiv='refresh' content='2;url=/status'>" : "")
                + "<title>Windy - " + title + "</title><style>body{margin:0;min-height:100vh;display:grid;place-items:center;background:#101725;color:#e9f0fa;font:16px system-ui}main{max-width:480px;padding:40px}span{color:#73bcff}h1{font-size:28px}p{color:#aebed2;line-height:1.6}</style>"
                + "<main><span>Windy</span><h1>" + title + "</h1><p>" + message + "</p></main></html>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
        finally { exchange.close(); }
    }
    @Override public synchronized void close() {
        if (stage == Stage.CLOSED) return;
        stage = Stage.CLOSED;
        code.completeExceptionally(new CancellationException("Sign-in cancelled."));
        server.stop(0);
        requests.shutdownNow();
        cleanup.shutdownNow();
    }
}
