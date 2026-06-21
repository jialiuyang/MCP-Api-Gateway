package com.mcpg.web.startup;

import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.repository.ToolRepository;
import com.mcpg.web.service.ToolNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * One-shot loader that pre-populates the gateway with believable demo data
 * on first boot.
 *
 * <p>Runs only when:</p>
 * <ul>
 *   <li>{@code mcpg.demo.seed=true} (default in the dev profile); and</li>
 *   <li>The services table is empty - so live deployments are never
 *       overwritten and re-runs are idempotent.</li>
 * </ul>
 *
 * <p>The seed loads three representative services - <em>order-service</em>,
 * <em>user-service</em> and the public Petstore. Each gets a handful of
 * synthetic operations covering both READ and WRITE risk levels and a few
 * pre-promoted high-value tools so the HYBRID exposure mode actually shows
 * something interesting on first launch.</p>
 */
@Component
public class DemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    private final ServiceRepository serviceRepository;
    private final ToolRepository toolRepository;

    @Value("${mcpg.demo.seed:false}")
    private boolean enabled;

    public DemoSeedRunner(ServiceRepository serviceRepository,
                           ToolRepository toolRepository) {
        this.serviceRepository = serviceRepository;
        this.toolRepository = toolRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.debug("Demo seed disabled (mcpg.demo.seed=false)");
            return;
        }
        if (serviceRepository.count() > 0) {
            log.debug("Demo seed skipped: services table already has rows");
            return;
        }

        log.info("Seeding demo data (3 services, ~20 tools)");
        seedOrderService();
        seedUserService();
        seedPetstore();
        log.info("Demo seed complete. Visit /services in the console to explore.");
    }

    private void seedOrderService() {
        ServiceEntity svc = newService(
                "order-service",
                "Order Service",
                "http://order-service.demo.svc.cluster.local",
                "http://order-service.demo.svc.cluster.local/v3/api-docs",
                ServiceEntity.SourceType.NACOS,
                Environment.DEV);
        serviceRepository.save(svc);
        saveTools(svc, List.of(
                op("getOrder", "GET", "/orders/{id}", "Get order by id",
                        "Returns the full order, including line items and shipping address.",
                        "orders,read", true),
                op("listOrders", "GET", "/orders", "List orders",
                        "Paged listing of orders, filterable by status / date range.",
                        "orders,read", true),
                op("createOrder", "POST", "/orders", "Create a new order",
                        "Submits a draft order. Returns the persisted order with server-assigned id.",
                        "orders,write", false),
                op("cancelOrder", "POST", "/orders/{id}/cancel", "Cancel an order",
                        "Marks the order as cancelled. Reversal of payment runs asynchronously.",
                        "orders,write", false),
                op("refundOrder", "POST", "/orders/{id}/refund", "Refund an order",
                        "Full or partial refund. Subject to write-op guard.",
                        "orders,write,financial", false),
                op("getOrderEvents", "GET", "/orders/{id}/events", "Get order audit trail",
                        "Returns the event log for the given order id.",
                        "orders,read,audit", false)
        ));
    }

    private void seedUserService() {
        ServiceEntity svc = newService(
                "user-service",
                "User Service",
                "http://user-service.demo.svc.cluster.local",
                "http://user-service.demo.svc.cluster.local/v3/api-docs",
                ServiceEntity.SourceType.NACOS,
                Environment.DEV);
        serviceRepository.save(svc);
        saveTools(svc, List.of(
                op("getUser", "GET", "/users/{id}", "Get user by id",
                        "Returns the user profile. PII fields may be redacted by policy.",
                        "users,read", true),
                op("searchUsers", "GET", "/users", "Search users",
                        "Search by email, name fragment or organization.",
                        "users,read", true),
                op("updateUserProfile", "PATCH", "/users/{id}", "Update profile",
                        "Partial update; only the fields supplied are modified.",
                        "users,write", false),
                op("deactivateUser", "POST", "/users/{id}/deactivate", "Deactivate user",
                        "Soft delete. Account is recoverable for 30 days.",
                        "users,write,sensitive", false),
                op("listUserSessions", "GET", "/users/{id}/sessions", "List active sessions",
                        "Returns active login sessions for the given user.",
                        "users,read,auth", false)
        ));
    }

    private void seedPetstore() {
        ServiceEntity svc = newService(
                "petstore",
                "Petstore (public)",
                "https://petstore3.swagger.io/api/v3",
                "https://petstore3.swagger.io/api/v3/openapi.json",
                ServiceEntity.SourceType.MANUAL,
                Environment.DEV);
        serviceRepository.save(svc);
        saveTools(svc, List.of(
                op("findPetsByStatus", "GET", "/pet/findByStatus", "Find pets by status",
                        "Multi-value status filter; returns matching pets.",
                        "pets,read", true),
                op("getPetById", "GET", "/pet/{petId}", "Find pet by id",
                        "Returns a single pet by id.",
                        "pets,read", false),
                op("addPet", "POST", "/pet", "Add a new pet",
                        "Add a new pet to the store.",
                        "pets,write", false),
                op("updatePet", "PUT", "/pet", "Update an existing pet",
                        "Update an existing pet by id.",
                        "pets,write", false),
                op("deletePet", "DELETE", "/pet/{petId}", "Delete a pet",
                        "Permanently delete a pet by id.",
                        "pets,write,delete", false)
        ));
    }

    private ServiceEntity newService(String name, String displayName, String baseUrl,
                                      String specUrl, ServiceEntity.SourceType source,
                                      Environment env) {
        ServiceEntity s = new ServiceEntity();
        s.setName(name);
        s.setDisplayName(displayName);
        s.setBaseUrl(baseUrl);
        s.setSpecUrl(specUrl);
        s.setSourceType(source);
        s.setSourceRef(source == ServiceEntity.SourceType.MANUAL ? specUrl
                : "demo/default/" + name);
        s.setEnvironment(env);
        s.setStatus(ServiceEntity.Status.ACTIVE);
        s.setToolCount(0);
        s.setLastSyncedAt(Instant.now());
        return s;
    }

    private void saveTools(ServiceEntity svc, List<OpSeed> ops) {
        for (OpSeed op : ops) {
            ToolEntity t = new ToolEntity();
            t.setServiceId(svc.getId());
            t.setOperationId(op.opId);
            t.setHttpMethod(op.method);
            t.setPath(op.path);
            t.setSummary(op.summary);
            t.setDescription(op.description);
            t.setTags(op.tags);
            t.setToolName(ToolNaming.toolName(svc.getName(), op.opId));
            t.setInputSchemaJson(stubInputSchema(op));
            t.setOutputSchemaJson("""
                    {"type":"object","description":"Stub demo response."}
                    """.trim());
            t.setRiskLevel(ToolNaming.inferRiskLevel(op.method, op.opId, op.path));
            t.setPromoted(op.promoted);
            t.setDeprecated(false);
            toolRepository.save(t);
        }
        svc.setToolCount(ops.size());
        serviceRepository.save(svc);
    }

    /** Minimal input schema so the meta tools have something to show. */
    private static String stubInputSchema(OpSeed op) {
        boolean hasPathParam = op.path.contains("{");
        if (hasPathParam) {
            return """
                    {"type":"object",
                     "properties":{"id":{"type":"string","description":"Path identifier."}},
                     "required":["id"]}
                    """.trim();
        }
        if ("POST".equals(op.method) || "PUT".equals(op.method) || "PATCH".equals(op.method)) {
            return """
                    {"type":"object",
                     "properties":{"payload":{"type":"object","description":"Request body."}},
                     "required":["payload"]}
                    """.trim();
        }
        return """
                {"type":"object","properties":{}}
                """.trim();
    }

    private static OpSeed op(String opId, String method, String path, String summary,
                              String description, String tags, boolean promoted) {
        return new OpSeed(opId, method, path, summary, description, tags, promoted);
    }

    private record OpSeed(String opId, String method, String path,
                           String summary, String description, String tags,
                           boolean promoted) {
    }
}
