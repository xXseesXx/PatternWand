package com.patternwand.patterns.scripted;

/**
 * Exception thrown when script compilation fails.
 */
public class ScriptCompileException extends Exception {

    private final String scriptName;

    public ScriptCompileException(String scriptName, String message) {
        super(message);
        this.scriptName = scriptName;
    }

    public ScriptCompileException(String scriptName, String message, Throwable cause) {
        super(message, cause);
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }

    @Override
    public String toString() {
        return "Script compilation error in '" + scriptName + "': " + getMessage();
    }
}
