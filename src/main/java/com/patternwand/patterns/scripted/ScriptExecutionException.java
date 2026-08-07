package com.patternwand.patterns.scripted;

/**
 * Exception thrown when script execution fails.
 */
public class ScriptExecutionException extends Exception {

    private final String scriptName;

    public ScriptExecutionException(String scriptName, String message) {
        super(message);
        this.scriptName = scriptName;
    }

    public ScriptExecutionException(String scriptName, String message, Throwable cause) {
        super(message, cause);
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }

    @Override
    public String toString() {
        return "Script execution error in '" + scriptName + "': " + getMessage();
    }
}
