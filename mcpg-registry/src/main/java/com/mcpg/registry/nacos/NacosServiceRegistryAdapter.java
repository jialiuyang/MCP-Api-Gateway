package com.mcpg.registry.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.mcpg.core.exception.McpgException;
import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.Environment;
import com.mcpg.core.model.RegistryConfig;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Nacos-backed {@link ServiceRegistryAdapter}.
 *
 * <p>The adapter is stateless: every public method opens a short-lived
 * {@link NamingService} client, performs the operation, and closes it. This
 * keeps the {@link RegistryConfig} input authoritative and avoids stale
 * connections when the operator edits the endpoint or credentials.</p>
 *
 * <h3>Config conventions</h3>
 * <ul>
 *   <li>{@code endpoint} - {@code host:port} (Nacos default 8848). Comma
 *       separated for cluster addresses.</li>
 *   <li>{@code username} / {@code password} - optional Nacos auth.</li>
 *   <li>{@code extra.namespace} - Nacos namespace id.</li>
 *   <li>{@code extra.group} - Nacos group, defaults to {@code DEFAULT_GROUP}.</li>
 * </ul>
 *
 * <p>HTTPS instances win over plain HTTP, and IP-based base URLs are
 * preferred when {@code instance.getIp()} is a public-looking address. The
 * gateway's discovery layer treats {@code baseUrls} as candidates rather
 * than absolute facts; whichever returns a parsable Swagger document first
 * becomes the service's effective base URL.</p>
 */
@Component
public class NacosServiceRegistryAdapter implements ServiceRegistryAdapter {

    private static final Logger log = LoggerFactory.getLogger(NacosServiceRegistryAdapter.class);

    /** Limit per registry-call to avoid pathological tenants with 10k+ services. */
    private static final int MAX_SERVICES_PER_CALL = 500;

    @Override
    public String getType() {
        return "nacos";
    }

    @Override
    public boolean testConnection(RegistryConfig config) {
        NamingService client = null;
        try {
            client = open(config);
            // Nacos 2.x does not expose a dedicated ping; listing service names
            // with a tiny page acts as a cheap reachability + auth probe.
            ListView<String> listed = client.getServicesOfServer(1, 1, group(config));
            log.debug("Nacos test ok at {} (total={})", config.getEndpoint(), listed.getCount());
            return true;
        } catch (NacosException e) {
            throw new McpgException("Nacos connection failed: " + e.getErrMsg(), e);
        } finally {
            shutdownQuietly(client);
        }
    }

    @Override
    public List<DiscoveredService> listServices(RegistryConfig config) {
        NamingService client = null;
        try {
            client = open(config);
            ListView<String> listed = client.getServicesOfServer(1, MAX_SERVICES_PER_CALL, group(config));
            List<String> names = listed.getData();
            log.info("Nacos returned {} service names (endpoint={}, group={})",
                    names.size(), config.getEndpoint(), group(config));

            List<DiscoveredService> out = new ArrayList<>(names.size());
            for (String serviceName : names) {
                List<Instance> instances;
                try {
                    instances = client.selectInstances(serviceName, group(config), true);
                } catch (NacosException e) {
                    log.warn("Skipping service '{}' - selectInstances failed: {}", serviceName, e.getErrMsg());
                    continue;
                }
                if (instances == null || instances.isEmpty()) continue;

                List<String> baseUrls = new ArrayList<>(instances.size());
                Map<String, String> firstMeta = null;
                for (Instance instance : instances) {
                    String url = buildBaseUrl(instance);
                    if (url != null && !baseUrls.contains(url)) baseUrls.add(url);
                    if (firstMeta == null && instance.getMetadata() != null) {
                        firstMeta = new HashMap<>(instance.getMetadata());
                    }
                }
                if (baseUrls.isEmpty()) continue;

                out.add(DiscoveredService.builder()
                        .name(serviceName)
                        .baseUrls(baseUrls)
                        .sourceType(getType())
                        .sourceRef(group(config) + ":" + serviceName)
                        .environment(envOf(config))
                        .metadata(firstMeta == null ? Map.of() : firstMeta)
                        .build());
            }
            return out;
        } catch (NacosException e) {
            throw new McpgException("Nacos listServices failed: " + e.getErrMsg(), e);
        } finally {
            shutdownQuietly(client);
        }
    }

    private NamingService open(RegistryConfig config) throws NacosException {
        Properties props = new Properties();
        props.setProperty(PropertyKeyConst.SERVER_ADDR, config.getEndpoint());
        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            props.setProperty(PropertyKeyConst.USERNAME, config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            props.setProperty(PropertyKeyConst.PASSWORD, config.getPassword());
        }
        String ns = config.getExtra().get("namespace");
        if (ns != null && !ns.isBlank()) {
            props.setProperty(PropertyKeyConst.NAMESPACE, ns);
        }
        return NamingFactory.createNamingService(props);
    }

    private String group(RegistryConfig config) {
        String g = config.getExtra().get("group");
        return (g == null || g.isBlank()) ? "DEFAULT_GROUP" : g;
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

    /**
     * Build an HTTP base URL from a Nacos {@link Instance}. Honours the
     * {@code secure}, {@code scheme} and {@code contextPath} metadata that
     * Spring Cloud Alibaba publishes so HTTPS services aren't misrepresented
     * as plain HTTP.
     */
    private String buildBaseUrl(Instance instance) {
        if (instance.getIp() == null || instance.getIp().isBlank()) return null;
        if (instance.getPort() <= 0) return null;

        Map<String, String> meta = instance.getMetadata();
        boolean secure = meta != null && Boolean.parseBoolean(
                meta.getOrDefault("secure", String.valueOf(instance.isEphemeral() ? false : false)));
        String scheme = meta == null ? null : meta.get("scheme");
        if (scheme == null) scheme = secure ? "https" : "http";

        String contextPath = (meta == null) ? "" : meta.getOrDefault("contextPath", "");
        if (contextPath == null) contextPath = "";
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) contextPath = "/" + contextPath;

        return scheme + "://" + instance.getIp() + ":" + instance.getPort() + contextPath;
    }

    private void shutdownQuietly(NamingService client) {
        if (client == null) return;
        try {
            client.shutDown();
        } catch (NacosException e) {
            log.debug("Nacos client shutdown failed (ignored): {}", e.getErrMsg());
        }
    }
}
