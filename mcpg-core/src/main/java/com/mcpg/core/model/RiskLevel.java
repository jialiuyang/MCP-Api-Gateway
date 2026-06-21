package com.mcpg.core.model;

/**
 * Coarse risk classification of a backend operation.
 *
 * <p>Used by the governance module to decide whether an invocation requires
 * approval, additional logging, or should be forbidden entirely.</p>
 */
public enum RiskLevel {

    /** Read-only operations. Auto-allowed in all environments. */
    READ,

    /** Low-risk writes (idempotent, easily reversible). Auto-allowed in dev/staging. */
    WRITE_LOW,

    /** High-risk writes (state mutating, hard to reverse). Requires confirmation. */
    WRITE_HIGH,

    /** Permanently forbidden from the gateway. Used for kill-switches. */
    FORBIDDEN
}
