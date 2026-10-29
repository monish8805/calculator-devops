package com.example.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Starts the real server on a free port and calls it over HTTP. */
class CalculatorApiIntegrationTest {

    private static CalculatorServer server;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws IOException {
        server = new CalculatorServer(0);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        URI uri = URI.create("http://localhost:" + server.getPort() + path);
        return client.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void addReturnsJsonResult() throws Exception {
        HttpResponse<String> r = get("/api/v1/add?a=2&b=3");
        assertEquals(200, r.statusCode());
        assertEquals("{\"operation\":\"add\",\"a\":2.0,\"b\":3.0,\"result\":5.0}", r.body());
    }

    @Test
    void divideEndpointWorksEndToEnd() throws Exception {
        HttpResponse<String> r = get("/api/v1/divide?a=10&b=4");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"result\":2.5"));
    }

    @Test
    void divideByZeroReturns400WithErrorBody() throws Exception {
        HttpResponse<String> r = get("/api/v1/divide?a=4&b=0");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"error\":\"DIVISION_BY_ZERO\""));
    }

    @Test
    void missingParameterReturns400() throws Exception {
        HttpResponse<String> r = get("/api/v1/subtract?a=4");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"error\":\"MISSING_PARAMETER\""));
    }

    @Test
    void nonNumericParameterReturns400() throws Exception {
        HttpResponse<String> r = get("/api/v1/multiply?a=abc&b=2");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"error\":\"INVALID_PARAMETER\""));
    }

    @Test
    void unknownOperationReturns404() throws Exception {
        assertEquals(404, get("/api/v1/power?a=2&b=3").statusCode());
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        HttpResponse<String> r = get("/health");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("UP"));
    }
}
