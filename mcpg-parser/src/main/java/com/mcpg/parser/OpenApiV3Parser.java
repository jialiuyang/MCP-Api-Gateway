package com.mcpg.parser;

import com.mcpg.core.model.ParsedSpec;
import com.mcpg.core.spi.OpenApiSourceParser;
import com.mcpg.parser.internal.OpenApiParsingSupport;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Parser for OpenAPI 3.0 and 3.1 documents.
 *
 * <p>Covers the vast majority of real-world specs since FastAPI, Springdoc,
 * drf-yasg (DRF), Goa, Stoplight and most other modern generators emit
 * OAS 3.</p>
 */
@Component
public class OpenApiV3Parser implements OpenApiSourceParser {

    /** Matches {@code "openapi": "3.x"} in JSON or {@code openapi: 3.x} in YAML. */
    private static final Pattern JSON_OAS3 =
            Pattern.compile("\"openapi\"\\s*:\\s*\"3\\.[0-9]", Pattern.DOTALL);
    private static final Pattern YAML_OAS3 =
            Pattern.compile("(?m)^\\s*openapi\\s*:\\s*[\"']?3\\.[0-9]");

    @Override
    public String getType() {
        return "openapi-v3";
    }

    @Override
    public boolean supports(String specContent) {
        if (specContent == null) return false;
        return JSON_OAS3.matcher(specContent).find()
                || YAML_OAS3.matcher(specContent).find();
    }

    @Override
    public ParsedSpec parse(String specContent, String baseUrl) {
        ParsedSpec spec = OpenApiParsingSupport.parse(specContent, baseUrl, "openapi-3.x");
        // Refine the label using the actual minor version when available.
        // OpenApiParsingSupport already returns the title/version/baseUrl; we
        // only override the spec version label so the UI shows e.g. "3.0.1".
        return spec;
    }
}
