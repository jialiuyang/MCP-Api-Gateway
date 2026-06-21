package com.mcpg.parser;

import com.mcpg.core.model.ParsedSpec;
import com.mcpg.core.spi.OpenApiSourceParser;
import com.mcpg.parser.internal.OpenApiParsingSupport;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Parser for Swagger 2.0 documents.
 *
 * <p>Implementation detail: the underlying {@code swagger-parser} library
 * up-converts Swagger 2.0 to OpenAPI 3 before returning the model object.
 * This bean is still useful because:</p>
 *
 * <ul>
 *   <li>It declares first-class support for Swagger 2 in
 *       {@link #supports(String)}, so the registry can distinguish dialects
 *       in logs and UI.</li>
 *   <li>It tags the resulting {@link ParsedSpec} with {@code swagger-2.0}
 *       so downstream consumers can adjust behavior (e.g. some specs of this
 *       era have looser type information).</li>
 * </ul>
 */
@Component
public class OpenApiV2Parser implements OpenApiSourceParser {

    private static final Pattern JSON_SWAGGER2 =
            Pattern.compile("\"swagger\"\\s*:\\s*\"2\\.0\"", Pattern.DOTALL);
    private static final Pattern YAML_SWAGGER2 =
            Pattern.compile("(?m)^\\s*swagger\\s*:\\s*[\"']?2\\.0");

    @Override
    public String getType() {
        return "swagger-v2";
    }

    @Override
    public boolean supports(String specContent) {
        if (specContent == null) return false;
        return JSON_SWAGGER2.matcher(specContent).find()
                || YAML_SWAGGER2.matcher(specContent).find();
    }

    @Override
    public ParsedSpec parse(String specContent, String baseUrl) {
        return OpenApiParsingSupport.parse(specContent, baseUrl, "swagger-2.0");
    }
}
