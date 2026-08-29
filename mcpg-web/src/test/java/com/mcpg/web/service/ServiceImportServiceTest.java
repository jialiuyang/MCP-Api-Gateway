package com.mcpg.web.service;

import com.mcpg.core.model.Environment;
import com.mcpg.web.dto.ImportResultDto;
import com.mcpg.web.dto.ImportSwaggerRequest;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.repository.ToolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of the manual Swagger import flow. The HTTP fetch step is
 * replaced with a fixed in-memory spec via a Mockito-backed
 * {@link SwaggerFetcher} bean so the test stays self-contained.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceImportServiceTest {

    private static final String SAMPLE_SPEC = """
            {
              "openapi": "3.0.1",
              "info": { "title": "Demo Service", "version": "1.0.0" },
              "servers": [ { "url": "https://demo.example.com" } ],
              "paths": {
                "/items": {
                  "get": {
                    "operationId": "listItems",
                    "summary": "List items",
                    "responses": { "200": { "description": "ok" } }
                  },
                  "post": {
                    "operationId": "createItem",
                    "summary": "Create item",
                    "requestBody": {
                      "required": true,
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "required": ["name"],
                            "properties": {
                              "name": { "type": "string" }
                            }
                          }
                        }
                      }
                    },
                    "responses": { "201": { "description": "ok" } }
                  }
                },
                "/items/{id}": {
                  "delete": {
                    "operationId": "deleteItem",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true,
                        "schema": { "type": "string" } }
                    ],
                    "responses": { "204": { "description": "ok" } }
                  }
                }
              }
            }
            """;

    @MockBean
    SwaggerFetcher fetcher;

    @Autowired
    ServiceImportService importService;

    @Autowired
    ServiceRepository serviceRepository;

    @Autowired
    ToolRepository toolRepository;

    @Test
    void importFromUrl_creates_service_and_tools() {
        when(fetcher.fetch(anyString())).thenReturn(
                new SwaggerFetcher.Result(SAMPLE_SPEC, "https://demo.example.com/openapi.json"));

        ImportSwaggerRequest req = new ImportSwaggerRequest();
        req.setName("demo");
        req.setUrl("https://demo.example.com/openapi.json");
        req.setEnvironment(Environment.DEV);

        ImportResultDto result = importService.importFromUrl(req);

        assertThat(result.getToolCount()).isEqualTo(3);
        assertThat(result.getService().getBaseUrl()).isEqualTo("https://demo.example.com");

        ServiceEntity service = serviceRepository
                .findByNameAndEnvironment("demo", Environment.DEV).orElseThrow();
        assertThat(service.getToolCount()).isEqualTo(3);
        assertThat(service.getStatus()).isEqualTo(ServiceEntity.Status.ACTIVE);

        List<ToolEntity> tools = toolRepository.findByServiceId(service.getId());
        assertThat(tools).hasSize(3);
        assertThat(tools)
                .extracting(ToolEntity::getToolName)
                .containsExactlyInAnyOrder("demo__listItems", "demo__createItem", "demo__deleteItem");
    }

    @Test
    void importFromUrl_is_idempotent() {
        when(fetcher.fetch(anyString())).thenReturn(
                new SwaggerFetcher.Result(SAMPLE_SPEC, "https://demo.example.com/openapi.json"));

        ImportSwaggerRequest req = new ImportSwaggerRequest();
        req.setName("demo-idem");
        req.setUrl("https://demo.example.com/openapi.json");
        req.setEnvironment(Environment.DEV);

        importService.importFromUrl(req);
        ImportResultDto second = importService.importFromUrl(req);

        assertThat(second.getToolCount()).isEqualTo(3);
        assertThat(second.getAdded()).isZero();
        assertThat(second.getUpdated()).isEqualTo(3);
        assertThat(second.getRemoved()).isZero();
    }

    /**
     * Two operations can collapse to the same {@code tool_name} (duplicate
     * operationId, or truncation). The first import inserts one row; the
     * second pass must update that row instead of inserting again, or
     * {@code uk_tool_name} blows up and the Nacos re-discover UI hangs on
     * fallback spec URLs.
     */
    @Test
    void importFromUrl_secondPass_reuses_row_when_operationIds_collide() {
        String spec = """
                {
                  "openapi": "3.0.1",
                  "info": { "title": "Dup Ops", "version": "1.0.0" },
                  "servers": [ { "url": "https://dup.example.com" } ],
                  "paths": {
                    "/a": {
                      "get": {
                        "operationId": "listItems",
                        "summary": "List A",
                        "responses": { "200": { "description": "ok" } }
                      }
                    },
                    "/b": {
                      "get": {
                        "operationId": "listItems",
                        "summary": "List B",
                        "responses": { "200": { "description": "ok" } }
                      }
                    }
                  }
                }
                """;
        when(fetcher.fetch(anyString())).thenReturn(
                new SwaggerFetcher.Result(spec, "https://dup.example.com/openapi.json"));

        ImportSwaggerRequest req = new ImportSwaggerRequest();
        req.setName("dup-ops");
        req.setUrl("https://dup.example.com/openapi.json");
        req.setEnvironment(Environment.DEV);

        importService.importFromUrl(req);
        ImportResultDto second = importService.importFromUrl(req);

        assertThat(second.getToolCount()).isEqualTo(1);
        List<ToolEntity> tools = toolRepository.findByServiceId(
                serviceRepository.findByNameAndEnvironment("dup-ops", Environment.DEV)
                        .orElseThrow()
                        .getId());
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getToolName()).isEqualTo("dup_ops__listItems");
    }
}
