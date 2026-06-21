/**
 * Core abstractions of MCP Gateway Enterprise.
 *
 * <h2>Package Layout</h2>
 * <ul>
 *   <li>{@code com.mcpg.core.model}   - Plain domain models shared across modules.</li>
 *   <li>{@code com.mcpg.core.spi}     - SPI interfaces for extensibility
 *       (service registry adapters, OpenAPI parsers, tool invokers).</li>
 *   <li>{@code com.mcpg.core.exception} - Common exception hierarchy.</li>
 * </ul>
 *
 * <h2>Design Principle</h2>
 * This module deliberately avoids depending on Spring or any concrete
 * implementation library so that the SPI contracts remain implementation
 * agnostic. New registry providers, parsers and invokers can be plugged in
 * by depending only on this module.
 */
package com.mcpg.core;
