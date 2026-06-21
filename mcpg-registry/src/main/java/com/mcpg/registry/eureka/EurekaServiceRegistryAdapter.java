package com.mcpg.registry.eureka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpg.core.exception.McpgException;
import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.Environment;
import com.mcpg.core.model.RegistryConfig;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Eureka adapter that talks plain HTTP to the Eureka server's REST API.
 *
 * <p>The classic Spring Cloud Netflix {@code EurekaClient} pulls a Kafka-sized
 * dependency tree and starts a heartbeat loop in the background; this is
 * overkill for the gateway, which only needs a one-shot snapshot of the
 * current registry on each discovery tick. So we hit
 * {@code GET {endpoint}/eureka/apps} directly with Jackson.</p>
 *
 * <h3>Config conventions</h3>
 * <ul>
 *   <li>{@code endpoint} - base Eureka URL ({@code http://eureka.example:8761}).
 *       The {@code /eureka/apps} suffix is appended if not already present.</li>
 *   <li>{@code username} / {@code password} - HTTP Basic credentials.</li>
 *   <li>{@code extra.preferIpAddress=true} (default) - if the instance has both
 *       a hostname and IP, use the IP. Set to {@code false} to use hostnames.</li>
 * </ul>
 */
@Component
public class EurekaServiceRegistryAdapter implements ServiceRegistryAdapter {

    private static final Logger log = LoggerFactory.getLogger(EurekaServiceRegistryAdapter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getType() {
        return "eureka";
    }

    @Override
    public boolean testConnection(RegistryConfig config) {
        try {
            buildClient(config)
                    .get()
                    .uri(appsUrl(config))
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (Exception e) {
            throw new McpgException("Eureka connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DiscoveredService> listServices(RegistryConfig config) {
        String body;
        try {
            body = buildClient(config)
                    .get()
                    .uri(appsUrl(config))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new McpgException("Eureka listServices failed: " + e.getMessage(), e);
        }
        if (body == null || body.isBlank()) return List.of();

        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode applications = root.path("applications").path("application");
            if (!applications.isArray()) return List.of();

            List<DiscoveredService> out = new ArrayList<>();
            for (JsonNode app : applications) {
                String name = app.path("name").asText();
                if (name == null || name.isBlank()) continue;

                JsonNode instances = app.path("instance");
                if (!instances.isArray() || instances.isEmpty()) continue;

                List<String> baseUrls = new ArrayList<>();
                Map<String, String> firstMeta = Map.of();
                for (JsonNode inst : instances) {
                    if (!"UP".equalsIgnoreCase(inst.path("status").asText())) continue;
                    String url = buildBaseUrl(inst, preferIp(config));
                    if (url != null && !baseUrls.contains(url)) baseUrls.add(url);
                    if (firstMeta.isEmpty()) firstMeta = readMetadata(inst);
                }
                if (baseUrls.isEmpty()) continue;

                out.add(DiscoveredService.builder()
                        .name(name)
                        .baseUrls(baseUrls)
                        .sourceType(getType())
                        .sourceRef(name)
                        .environment(envOf(config))
                        .metadata(firstMeta)
                        .build());
            }
            log.info("Eureka returned {} healthy services at {}", out.size(), config.getEndpoint());
            return out;
        } catch (Exception e) {
            throw new McpgException("Eureka response parse failed: " + e.getMessage(), e);
        }
    }

    private RestClient buildClient(RegistryConfig config) {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            String token = config.getUsername() + ":"
                    + (config.getPassword() == null ? "" : config.getPassword());
            String encoded = Base64.getEncoder().encodeToString(
                    token.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return builder.build();
    }

    private String appsUrl(RegistryConfig config) {
        String base = config.getEndpoint();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!base.endsWith("/eureka/apps")) {
            if (base.endsWith("/eureka")) base = base + "/apps";
            else base = base + "/eureka/apps";
        }
        return base;
    }

    private boolean preferIp(RegistryConfig config) {
        String v = config.getExtra().get("preferIpAddress");
        return v == null || v.isBlank() || Boolean.parseBoolean(v);
    }

    private String buildBaseUrl(JsonNode instance, boolean preferIp) {
        // Eureka instances expose port + securePort with @enabled markers.
        JsonNode securePort = instance.path("securePort");
        boolean secureEnabled = "true".equalsIgnoreCase(securePort.path("@enabled").asText());
        int port;
        String scheme;
        if (secureEnabled && securePort.path("$").asInt(0) > 0) {
            port = securePort.path("$").asInt();
            scheme = "https";
        } else {
            port = instance.path("port").path("$").asInt(0);
            scheme = "http";
        }
        if (port <= 0) return null;

        String host = null;
        if (preferIp) {
            String ip = instance.path("ipAddr").asText(null);
            if (ip != null && !ip.isBlank()) host = ip;
        }
        if (host == null) host = instance.path("hostName").asText(null);
        if (host == null || host.isBlank()) return null;

        return scheme + "://" + host + ":" + port;
    }

    private Map<String, String> readMetadata(JsonNode instance) {
        JsonNode meta = instance.path("metadata");
        if (meta.isMissingNode() || !meta.isObject()) return Map.of();
        java.util.Map<String, String> out = new java.util.HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = meta.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            out.put(e.getKey(), e.getValue().asText());
        }
        return out;
    }

    private Environment envOf(RegistryConfig config) {
        String env = config.getExtra().get("environment");
        if (env == null) return Environment.UNKNOWN;
        try {
            return Environment.valueOf(env.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Environment.UNKNOWN;
        }
    }
}
