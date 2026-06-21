package com.mcpg.parser;

import com.mcpg.core.model.ParsedSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiV2ParserTest {

    private final OpenApiV2Parser parser = new OpenApiV2Parser();

    @Test
    void supports_recognizes_swagger2_json() {
        String spec = """
                { "swagger": "2.0", "info": {"title":"T","version":"1"}, "paths": {} }
                """;
        assertTrue(parser.supports(spec));
    }

    @Test
    void supports_rejects_oas3() {
        String spec = """
                { "openapi": "3.0.1", "info": {"title":"T","version":"1"}, "paths": {} }
                """;
        assertFalse(parser.supports(spec));
    }

    @Test
    void parse_converts_swagger2_path_and_query_params() {
        // swagger-parser internally up-converts to OAS3; we still exercise the
        // path so regressions in the conversion are caught.
        String spec = """
                {
                  "swagger": "2.0",
                  "info": { "title": "Legacy", "version": "1.0" },
                  "host": "legacy.example.com",
                  "basePath": "/api",
                  "schemes": ["https"],
                  "paths": {
                    "/users/{id}": {
                      "get": {
                        "operationId": "getUser",
                        "parameters": [
                          { "name": "id", "in": "path", "required": true, "type": "string" },
                          { "name": "verbose", "in": "query", "type": "boolean" }
                        ],
                        "responses": { "200": { "description": "ok" } }
                      }
                    }
                  }
                }
                """;
        ParsedSpec parsed = parser.parse(spec, null);
        assertEquals("swagger-2.0", parsed.getSpecVersion());
        assertEquals(1, parsed.getOperations().size());
        assertTrue(parsed.getBaseUrl() != null
                && parsed.getBaseUrl().contains("legacy.example.com"));
        var op = parsed.getOperations().get(0);
        assertEquals("getUser", op.getOperationId());
        assertEquals("GET", op.getHttpMethod());
    }
}
