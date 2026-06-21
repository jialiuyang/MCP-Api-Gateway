package com.mcpg.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcpg.core.model.ParsedOperation;
import com.mcpg.core.model.ParsedSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OpenApiV3Parser}.
 *
 * <p>The tests use hand-crafted minimal OpenAPI 3 documents to keep the
 * assertions sharp; we are testing the parsing/conversion code, not the
 * upstream swagger-parser library.</p>
 */
class OpenApiV3ParserTest {

    private final OpenApiV3Parser parser = new OpenApiV3Parser();

    @Test
    void supports_recognizes_oas3_json() {
        String spec = """
                {
                  "openapi": "3.0.1",
                  "info": { "title": "T", "version": "1" },
                  "paths": {}
                }
                """;
        assertTrue(parser.supports(spec));
    }

    @Test
    void supports_recognizes_oas3_yaml() {
        String spec = """
                openapi: 3.0.2
                info:
                  title: T
                  version: '1'
                paths: {}
                """;
        assertTrue(parser.supports(spec));
    }

    @Test
    void supports_rejects_swagger2() {
        String spec = """
                { "swagger": "2.0", "info": {"title":"T","version":"1"}, "paths": {} }
                """;
        assertFalse(parser.supports(spec));
    }

    @Test
    void parse_extracts_operations_with_input_schema() {
        String spec = """
                {
                  "openapi": "3.0.1",
                  "info": { "title": "Order Service", "version": "1.2.0" },
                  "servers": [ { "url": "https://api.example.com/orders" } ],
                  "paths": {
                    "/orders/{orderId}": {
                      "get": {
                        "operationId": "getOrder",
                        "summary": "Get order by id",
                        "parameters": [
                          { "name": "orderId", "in": "path", "required": true,
                            "schema": { "type": "string" } },
                          { "name": "expand", "in": "query",
                            "schema": { "type": "string", "enum": ["items","customer"] } }
                        ],
                        "responses": { "200": { "description": "ok" } }
                      }
                    },
                    "/orders": {
                      "post": {
                        "operationId": "createOrder",
                        "summary": "Create order",
                        "requestBody": {
                          "required": true,
                          "content": {
                            "application/json": {
                              "schema": {
                                "type": "object",
                                "required": ["sku"],
                                "properties": {
                                  "sku": { "type": "string" },
                                  "quantity": { "type": "integer" }
                                }
                              }
                            }
                          }
                        },
                        "responses": { "201": { "description": "created" } }
                      }
                    }
                  }
                }
                """;

        ParsedSpec parsed = parser.parse(spec, "https://fallback.example.com");

        assertEquals("Order Service", parsed.getTitle());
        assertEquals("1.2.0", parsed.getVersion());
        assertEquals("https://api.example.com/orders", parsed.getBaseUrl());
        assertEquals(2, parsed.getOperations().size());

        ParsedOperation get = findOp(parsed.getOperations(), "getOrder");
        assertEquals("GET", get.getHttpMethod());
        assertEquals("/orders/{orderId}", get.getPath());
        JsonNode input = get.getInputSchema();
        assertEquals("object", input.get("type").asText());
        assertTrue(input.get("properties").has("pathParams"));
        assertTrue(input.get("properties").has("queryParams"));
        assertEquals("string",
                input.get("properties").get("pathParams").get("properties").get("orderId").get("type").asText());
        assertTrue(input.get("required").toString().contains("pathParams"));

        ParsedOperation post = findOp(parsed.getOperations(), "createOrder");
        assertEquals("POST", post.getHttpMethod());
        JsonNode bodySchema = post.getInputSchema().get("properties").get("body");
        assertNotNull(bodySchema);
        assertEquals("object", bodySchema.get("type").asText());
        assertTrue(bodySchema.get("properties").has("sku"));
    }

    @Test
    void parse_synthesizes_operationId_when_missing() {
        String spec = """
                {
                  "openapi": "3.0.1",
                  "info": { "title": "T", "version": "1" },
                  "paths": {
                    "/items/{id}": {
                      "get": {
                        "responses": { "200": { "description": "ok" } }
                      }
                    }
                  }
                }
                """;
        ParsedSpec parsed = parser.parse(spec, "https://x");
        assertEquals(1, parsed.getOperations().size());
        assertEquals("get_items_id", parsed.getOperations().get(0).getOperationId());
    }

    @Test
    void parse_falls_back_to_baseUrl_when_servers_missing() {
        String spec = """
                {
                  "openapi": "3.0.1",
                  "info": { "title": "T", "version": "1" },
                  "paths": {}
                }
                """;
        ParsedSpec parsed = parser.parse(spec, "https://fallback.example.com");
        assertEquals("https://fallback.example.com", parsed.getBaseUrl());
    }

    private ParsedOperation findOp(List<ParsedOperation> ops, String id) {
        return ops.stream().filter(o -> id.equals(o.getOperationId())).findFirst()
                .orElseThrow(() -> new AssertionError("missing op: " + id));
    }
}
