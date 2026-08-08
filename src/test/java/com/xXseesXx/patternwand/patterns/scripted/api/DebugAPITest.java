package com.xXseesXx.patternwand.patterns.scripted.api;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for DebugAPI.
 * Tests debug message printing and enable/disable functionality.
 */
public class DebugAPITest {

    private DebugAPI debug;

    @Before
    public void setUp() {
        debug = new DebugAPI();
        // Ensure debug starts disabled
        DebugAPI.setDebugEnabled(false);
        DebugAPI.clearMessages();
    }

    @After
    public void tearDown() {
        // Clean up after tests
        DebugAPI.setDebugEnabled(false);
        DebugAPI.clearMessages();
    }

    @Test
    public void testDebugStartsDisabled() {
        assertFalse("Debug should start disabled", DebugAPI.isDebugEnabled());
    }

    @Test
    public void testEnableDebug() {
        DebugAPI.setDebugEnabled(true);
        assertTrue("Debug should be enabled", DebugAPI.isDebugEnabled());
    }

    @Test
    public void testDisableDebug() {
        DebugAPI.setDebugEnabled(true);
        DebugAPI.setDebugEnabled(false);
        assertFalse("Debug should be disabled", DebugAPI.isDebugEnabled());
    }

    @Test
    public void testPrintWhenDisabled() {
        DebugAPI.setDebugEnabled(false);
        debug.print("Test message");

        List<String> messages = DebugAPI.getMessages();
        assertEquals("No messages should be stored when disabled", 0, messages.size());
    }

    @Test
    public void testPrintWhenEnabled() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Test message");

        List<String> messages = DebugAPI.getMessages();
        assertEquals("Message should be stored when enabled", 1, messages.size());
        assertEquals("Test message", messages.get(0));
    }

    @Test
    public void testPrintMultipleMessages() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Message 1");
        debug.print("Message 2");
        debug.print("Message 3");

        List<String> messages = DebugAPI.getMessages();
        assertEquals(3, messages.size());
        assertEquals("Message 1", messages.get(0));
        assertEquals("Message 2", messages.get(1));
        assertEquals("Message 3", messages.get(2));
    }

    @Test
    public void testPrintEmptyString() {
        DebugAPI.setDebugEnabled(true);
        debug.print("");

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        assertEquals("", messages.get(0));
    }

    @Test
    public void testPrintWithMultipleValues() {
        DebugAPI.setDebugEnabled(true);
        debug.print("x:", 10, "y:", 20, "z:", 30);

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        // Values should be concatenated with spaces
        assertTrue(
            messages.get(0)
                .contains("x:"));
        assertTrue(
            messages.get(0)
                .contains("10"));
        assertTrue(
            messages.get(0)
                .contains("y:"));
        assertTrue(
            messages.get(0)
                .contains("20"));
        assertTrue(
            messages.get(0)
                .contains("z:"));
        assertTrue(
            messages.get(0)
                .contains("30"));
    }

    @Test
    public void testPrintWithDifferentTypes() {
        DebugAPI.setDebugEnabled(true);
        debug.print("String", 123, 45.67, true, false);

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        String message = messages.get(0);
        assertTrue(message.contains("String"));
        assertTrue(message.contains("123"));
        assertTrue(message.contains("45.67"));
        assertTrue(message.contains("true"));
        assertTrue(message.contains("false"));
    }

    @Test
    public void testClearMessages() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Message 1");
        debug.print("Message 2");

        assertEquals(
            2,
            DebugAPI.getMessages()
                .size());

        DebugAPI.clearMessages();

        assertEquals(
            "Messages should be cleared",
            0,
            DebugAPI.getMessages()
                .size());
    }

    @Test
    public void testDisablingClearsMessages() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Message 1");
        debug.print("Message 2");

        assertEquals(
            2,
            DebugAPI.getMessages()
                .size());

        DebugAPI.setDebugEnabled(false);

        assertEquals(
            "Disabling should clear messages",
            0,
            DebugAPI.getMessages()
                .size());
    }

    @Test
    public void testGetMessagesReturnsNewList() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Message 1");

        List<String> messages1 = DebugAPI.getMessages();
        List<String> messages2 = DebugAPI.getMessages();

        assertNotSame("Should return different list instances", messages1, messages2);
        assertEquals("Should have same content", messages1, messages2);
    }

    @Test
    public void testPrintAfterClear() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Message 1");
        DebugAPI.clearMessages();
        debug.print("Message 2");

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        assertEquals("Message 2", messages.get(0));
    }

    @Test
    public void testMultipleDebugInstances() {
        DebugAPI.setDebugEnabled(true);

        DebugAPI debug1 = new DebugAPI();
        DebugAPI debug2 = new DebugAPI();

        debug1.print("From debug1");
        debug2.print("From debug2");

        List<String> messages = DebugAPI.getMessages();
        assertEquals("Messages from all instances should be collected", 2, messages.size());
        assertTrue(messages.contains("From debug1"));
        assertTrue(messages.contains("From debug2"));
    }

    @Test
    public void testEnableDisableMultipleTimes() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Message 1");

        DebugAPI.setDebugEnabled(false);
        debug.print("Message 2"); // Should not be stored

        DebugAPI.setDebugEnabled(true);
        debug.print("Message 3");

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size()); // Only Message 3, as disable clears
        assertEquals("Message 3", messages.get(0));
    }

    @Test
    public void testPrintNullValue() {
        DebugAPI.setDebugEnabled(true);
        debug.print("Value:", null, "End");

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        assertTrue(
            messages.get(0)
                .contains("null"));
    }

    @Test
    public void testPrintLargeMessage() {
        DebugAPI.setDebugEnabled(true);

        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            large.append("x");
        }

        debug.print(large.toString());

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        assertEquals(
            1000,
            messages.get(0)
                .length());
    }

    @Test
    public void testPrintManyMessages() {
        DebugAPI.setDebugEnabled(true);

        for (int i = 0; i < 100; i++) {
            debug.print("Message " + i);
        }

        List<String> messages = DebugAPI.getMessages();
        assertEquals(100, messages.size());

        // Verify order is preserved
        for (int i = 0; i < 100; i++) {
            assertEquals("Message " + i, messages.get(i));
        }
    }

    @Test
    public void testPrintWithNoArguments() {
        DebugAPI.setDebugEnabled(true);
        debug.print(new Object[0]);

        List<String> messages = DebugAPI.getMessages();
        assertEquals(1, messages.size());
        assertEquals("", messages.get(0));
    }

    @Test
    public void testStaticStateSharedAcrossInstances() {
        DebugAPI debug1 = new DebugAPI();
        DebugAPI debug2 = new DebugAPI();

        // Enable through static method
        DebugAPI.setDebugEnabled(true);

        debug1.print("Test 1");
        debug2.print("Test 2");

        // Both should have added messages
        List<String> messages = DebugAPI.getMessages();
        assertEquals(2, messages.size());

        // Disable through static method
        DebugAPI.setDebugEnabled(false);

        // Messages should be cleared for all instances
        assertEquals(
            0,
            DebugAPI.getMessages()
                .size());
    }
}
