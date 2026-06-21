package com.mcpg.core.model;

/**
 * Strategy for how a service's tools are exposed through MCP to AI clients.
 *
 * <p>The trade-off is between <b>tool count</b> and <b>directness</b>:</p>
 * <ul>
 *   <li>Direct exposure means every operation becomes a first-class MCP tool,
 *       maximally discoverable but easily exceeding the LLM context budget
 *       when many services are aggregated.</li>
 *   <li>Meta tools collapse all operations behind {@code search_api} and
 *       {@code call_api}, keeping the tool surface tiny at the cost of an
 *       extra LLM round-trip for discovery.</li>
 * </ul>
 */
public enum ExposureMode {

    /**
     * Only the four meta tools ({@code list_services}, {@code search_api},
     * {@code get_api_schema}, {@code call_api}) are exposed regardless of how
     * many backend operations exist. Recommended default when the gateway
     * aggregates many services.
     */
    META,

    /**
     * Every operation from every discovered service is exposed as an
     * individual MCP tool. Easiest for the LLM to invoke directly but blows
     * up tool count quickly. Suitable for small demos or single-service setups.
     */
    DIRECT_ALL,

    /**
     * Hybrid: meta tools are always exposed, but operations that have been
     * explicitly <i>promoted</i> by an operator (see {@code promoted} flag on
     * a tool) are additionally surfaced as direct tools. Provides a path to
     * organically grow direct exposure based on real usage patterns.
     */
    HYBRID
}
