package com.mcpg.web.service;

import com.mcpg.core.exception.ParserNotFoundException;
import com.mcpg.core.spi.OpenApiSourceParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central directory of {@link OpenApiSourceParser} beans.
 *
 * <p>Spring auto-collects all parser beans on the classpath at startup; this
 * component routes a piece of spec content to the first parser that claims
 * to support it.</p>
 */
@Component
public class ParserRegistry {

    private final List<OpenApiSourceParser> parsers;
    private final Map<String, OpenApiSourceParser> byType;

    public ParserRegistry(List<OpenApiSourceParser> parsers) {
        this.parsers = List.copyOf(parsers);
        this.byType = this.parsers.stream()
                .collect(Collectors.toUnmodifiableMap(OpenApiSourceParser::getType, p -> p));
    }

    /** Pick a parser by content sniffing. */
    public OpenApiSourceParser pick(String specContent) {
        for (OpenApiSourceParser p : parsers) {
            if (p.supports(specContent)) return p;
        }
        throw new ParserNotFoundException(
                "No parser claims to support the provided spec. "
                        + "Available parsers: " + byType.keySet());
    }

    /** All registered parsers (used by the UI to show supported dialects). */
    public List<String> supportedTypes() {
        return parsers.stream().map(OpenApiSourceParser::getType).toList();
    }
}
