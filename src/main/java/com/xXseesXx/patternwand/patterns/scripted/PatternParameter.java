package com.xXseesXx.patternwand.patterns.scripted;

/**
 * Represents a configurable parameter for a pattern script.
 */
public class PatternParameter {

    public enum Type {
        INTEGER, // Whole numbers
        FLOAT, // Decimal numbers
        BOOLEAN, // true/false
        STRING // Text
    }

    private final String name;
    private final Type type;
    private final Object defaultValue;
    private final Double min;
    private final Double max;

    /**
     * Create a new pattern parameter.
     *
     * @param name         Parameter name
     * @param type         Parameter type
     * @param defaultValue Default value
     * @param min          Minimum value (for numeric types only, can be null)
     * @param max          Maximum value (for numeric types only, can be null)
     */
    public PatternParameter(String name, Type type, Object defaultValue, Double min, Double max) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    /**
     * Validate and coerce a value for this parameter.
     *
     * @param value Value to validate
     * @return Validated and coerced value
     * @throws IllegalArgumentException If value is invalid
     */
    public Object validate(Object value) {
        if (value == null) {
            return defaultValue;
        }

        switch (type) {
            case INTEGER:
                int intVal;
                if (value instanceof Number) {
                    intVal = ((Number) value).intValue();
                } else {
                    try {
                        // Try parsing as integer first
                        intVal = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        // Try as double then truncate
                        try {
                            intVal = (int) Double.parseDouble(value.toString());
                        } catch (NumberFormatException e2) {
                            throw new IllegalArgumentException(
                                "Parameter '" + name + "' must be an integer, got: " + value);
                        }
                    }
                }

                // Apply constraints
                if (min != null && intVal < min.intValue()) {
                    intVal = min.intValue();
                }
                if (max != null && intVal > max.intValue()) {
                    intVal = max.intValue();
                }

                return intVal;

            case FLOAT:
                double floatVal;
                if (value instanceof Number) {
                    floatVal = ((Number) value).doubleValue();
                } else {
                    try {
                        floatVal = Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Parameter '" + name + "' must be a number, got: " + value);
                    }
                }

                // Apply constraints
                if (min != null && floatVal < min) {
                    floatVal = min;
                }
                if (max != null && floatVal > max) {
                    floatVal = max;
                }

                return floatVal;

            case BOOLEAN:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof String) {
                    String str = ((String) value).toLowerCase();
                    return str.equals("true") || str.equals("yes") || str.equals("1");
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue() != 0.0;
                }
                return defaultValue;

            case STRING:
                return value.toString();

            default:
                return defaultValue;
        }
    }

    /**
     * Get a human-readable type name.
     */
    public String getTypeName() {
        switch (type) {
            case INTEGER:
                return "integer";
            case FLOAT:
                return "float";
            case BOOLEAN:
                return "boolean";
            case STRING:
                return "string";
            default:
                return "unknown";
        }
    }

    /**
     * Get a formatted string describing this parameter.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(name)
            .append(" (")
            .append(getTypeName());

        if (type == Type.INTEGER || type == Type.FLOAT) {
            if (min != null || max != null) {
                sb.append(", range: ");
                sb.append(min != null ? (type == Type.INTEGER ? min.intValue() : min) : "-∞");
                sb.append(" to ");
                sb.append(max != null ? (type == Type.INTEGER ? max.intValue() : max) : "∞");
            }
        }

        sb.append(", default: ")
            .append(defaultValue)
            .append(")");

        return sb.toString();
    }
}
