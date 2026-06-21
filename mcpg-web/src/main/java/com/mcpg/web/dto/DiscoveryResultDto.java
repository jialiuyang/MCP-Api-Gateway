package com.mcpg.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of a manual or scheduled discovery run.
 *
 * <p>Discovery is a noisy operation: hundreds of services might be
 * advertised, and only a subset might be reachable from the gateway. The
 * DTO splits the outcome by category so the operator can immediately see
 * which services need their Swagger URL configured by hand.</p>
 */
@Data
@Builder
public class DiscoveryResultDto {

    private long registryId;
    private String registryName;

    /** Total number of services the adapter returned. */
    private int discovered;

    /** Number of services successfully imported (Swagger fetched + parsed). */
    private int imported;

    /** Number of services whose Swagger could not be located / parsed. */
    private int skipped;

    private List<DiscoveredServiceOutcome> items;

    @Data
    @Builder
    public static class DiscoveredServiceOutcome {
        private String name;
        private String baseUrl;
        /** {@code IMPORTED}, {@code UPDATED}, or {@code SKIPPED}. */
        private String status;
        private String message;
    }
}
