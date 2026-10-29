package com.example.calculator;

import com.example.calculator.dto.CalculationResponse;
import com.example.calculator.dto.ErrorResponse;
import com.example.calculator.exception.DivideByZeroException;
import com.example.calculator.service.CalculatorService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

/** Calculator REST API built on the JDK's built-in HTTP server (no framework). */
public class CalculatorServer {

    private static final String API_PREFIX = "/api/v1/";

    private final CalculatorService calculator = new CalculatorService();
    private final HttpServer server;

    public CalculatorServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext(API_PREFIX, this::handleApi);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        new CalculatorServer(port).start();
        System.out.println("Calculator API listening on port " + port);
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, error(405, "METHOD_NOT_ALLOWED", "Only GET is supported"));
            return;
        }
        send(ex, 200, "{\"status\":\"UP\"}");
    }

    private void handleApi(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, error(405, "METHOD_NOT_ALLOWED", "Only GET is supported"));
            return;
        }
        String operation = ex.getRequestURI().getPath().substring(API_PREFIX.length());
        DoubleBinaryOperator fn = switch (operation) {
            case "add" -> calculator::add;
            case "subtract" -> calculator::subtract;
            case "multiply" -> calculator::multiply;
            case "divide" -> calculator::divide;
            default -> null;
        };
        if (fn == null) {
            send(ex, 404, error(404, "NOT_FOUND", "Unknown operation '" + operation + "'"));
            return;
        }

        try {
            Map<String, String> params = parseQuery(ex.getRequestURI().getRawQuery());
            double a = number(params, "a");
            double b = number(params, "b");
            CalculationResponse r = new CalculationResponse(operation, a, b, fn.applyAsDouble(a, b));
            send(ex, 200, "{\"operation\":\"" + r.operation() + "\",\"a\":" + r.a()
                    + ",\"b\":" + r.b() + ",\"result\":" + r.result() + "}");
        } catch (DivideByZeroException e) {
            send(ex, 400, error(400, "DIVISION_BY_ZERO", e.getMessage()));
        } catch (MissingParameterException e) {
            send(ex, 400, error(400, "MISSING_PARAMETER",
                    "Required parameter '" + e.getMessage() + "' is missing"));
        } catch (IllegalArgumentException e) {
            send(ex, 400, error(400, "INVALID_PARAMETER", e.getMessage()));
        }
    }

    private static double number(Map<String, String> params, String name) {
        String raw = params.get(name);
        if (raw == null || raw.isBlank()) {
            throw new MissingParameterException(name);
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a number");
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    private static String error(int status, String code, String message) {
        ErrorResponse e = new ErrorResponse(status, code, message);
        return "{\"status\":" + e.status() + ",\"error\":\"" + e.error()
                + "\",\"message\":\"" + escape(e.message()) + "\"}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static class MissingParameterException extends RuntimeException {
        MissingParameterException(String name) {
            super(name);
        }
    }
}
