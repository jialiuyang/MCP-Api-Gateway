package com.mcpg.core.model;

/**
 * Logical deployment environment of a discovered service or tool.
 * <p>
 * Used as the unit of governance for environment isolation (e.g. forbidding
 * Cursor from invoking write operations on {@link #PROD}).
 */
public enum Environment {

    /** Local / developer machine. Generally lowest restriction. */
    DEV,

    /** Staging / pre-release. Full feature set with non-prod data. */
    STAGING,

    /** Production. Highly restricted; write operations require approval. */
    PROD,

    /** Unclassified or not yet labeled. Treated as the most restrictive. */
    UNKNOWN
}
