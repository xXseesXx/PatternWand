package com.xXseesXx.patternwand.patterns.scripted;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for a pattern script, including name, author, and configurable parameters.
 */
public class PatternMetadata {

    private final String name;
    private final String author;
    private final String description;
    private final List<PatternParameter> parameters;
    private final Map<String, PatternParameter> parameterMap;
    private final boolean ignoreMetadata;

    /**
     * Create pattern metadata.
     *
     * @param name           Pattern display name
     * @param author         Pattern author
     * @param description    Pattern description
     * @param parameters     List of configurable parameters
     * @param ignoreMetadata If true, flood-fill ignores block metadata/rotation when matching
     */
    public PatternMetadata(String name, String author, String description, List<PatternParameter> parameters,
        boolean ignoreMetadata) {
        this.name = name != null ? name : "Unnamed Pattern";
        this.author = author != null ? author : "Unknown";
        this.description = description != null ? description : "";
        this.parameters = parameters != null ? parameters : Collections.<PatternParameter>emptyList();
        this.ignoreMetadata = ignoreMetadata;

        // Build parameter map for quick lookup
        this.parameterMap = new HashMap<String, PatternParameter>();
        for (PatternParameter param : this.parameters) {
            parameterMap.put(param.getName(), param);
        }
    }

    /**
     * Legacy constructor for backward compatibility.
     *
     * @deprecated Use {@link #PatternMetadata(String, String, String, List, boolean)} instead
     */
    @Deprecated
    public PatternMetadata(String name, String author, List<PatternParameter> parameters, boolean ignoreMetadata) {
        this(name, author, "", parameters, ignoreMetadata);
    }

    /**
     * Legacy constructor for backward compatibility.
     *
     * @deprecated Use {@link #PatternMetadata(String, String, String, List, boolean)} instead
     */
    @Deprecated
    public PatternMetadata(String name, String author, List<PatternParameter> parameters) {
        this(name, author, "", parameters, false);
    }

    /**
     * Create default metadata with no parameters.
     */
    public PatternMetadata() {
        this(null, null, null, null, false);
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public List<PatternParameter> getParameters() {
        return parameters;
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Check if this pattern should ignore block metadata/rotation during flood-fill.
     *
     * @return true if metadata should be ignored (only block type matters)
     */
    public boolean shouldIgnoreMetadata() {
        return ignoreMetadata;
    }

    /**
     * Get a parameter by name.
     *
     * @param name Parameter name
     * @return Parameter, or null if not found
     */
    public PatternParameter getParameter(String name) {
        return parameterMap.get(name);
    }

    /**
     * Create a parameter values map with defaults.
     *
     * @return Map of parameter name to default value
     */
    public Map<String, Object> createDefaultValues() {
        Map<String, Object> values = new HashMap<String, Object>();
        for (PatternParameter param : parameters) {
            values.put(param.getName(), param.getDefaultValue());
        }
        return values;
    }

    /**
     * Validate and apply parameter overrides.
     *
     * @param overrides Map of parameter names to override values
     * @return Map of validated parameter values
     */
    public Map<String, Object> applyOverrides(Map<String, Object> overrides) {
        Map<String, Object> values = createDefaultValues();

        if (overrides != null) {
            for (Map.Entry<String, Object> entry : overrides.entrySet()) {
                PatternParameter param = getParameter(entry.getKey());
                if (param != null) {
                    values.put(entry.getKey(), param.validate(entry.getValue()));
                }
            }
        }

        return values;
    }
}
