package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.*;

import hudson.util.ListBoxModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JVMVersionMonitorTest {
    private JVMVersionMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new JVMVersionMonitor();
        monitor.setDisconnect(false);
    }

    private static Object[] parameters() {
        return new Object[][] {
            {
                JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH,
            },
            {
                JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH,
            },
            {
                JVMVersionComparator.ComparisonMode.EXACT_MATCH,
            },
            {
                JVMVersionComparator.ComparisonMode.EXACT_MATCH,
            },
            {
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE,
            },
            {
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE,
            },
        };
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void checkComparisonMode(JVMVersionComparator.ComparisonMode comparisonMode) {
        JVMVersionMonitor object = new JVMVersionMonitor(comparisonMode);
        assertEquals(comparisonMode, object.getComparisonMode());
    }

    @Test
    public void checkToHtmlRendering() throws Exception {

        JVMVersionMonitor object = new JVMVersionMonitor(JVMVersionComparator.ComparisonMode.EXACT_MATCH);

        // N/A
        assertEquals("N/A", object.toHtml(null));
        assertEquals("N/A", object.toHtml("N/A"));

        // EXACT_MATCH
        assertEquals(
                Runtime.version().toString(), object.toHtml(Runtime.version().toString()));
        assertEquals(asError("1.1.1.1+1"), object.toHtml("1.1.1.1+1"));

        // RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE
        object = new JVMVersionMonitor(JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE);
        assertEquals(
                Runtime.version().toString(), object.toHtml(Runtime.version().toString()));
        assertEquals(majorGreater(), object.toHtml(majorGreater()));
        assertEquals(majorVersionMatch(), object.toHtml(majorVersionMatch()));
        assertEquals(asError(majorLower()), object.toHtml(majorLower()));

        // MAJOR_MINOR_MATCH
        object = new JVMVersionMonitor(JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH);
        assertEquals(
                Runtime.version().toString(), object.toHtml(Runtime.version().toString()));
        assertEquals(majorGreater(), object.toHtml(majorGreater()));
        assertEquals(majorVersionMatch(), object.toHtml(majorVersionMatch()));
        assertEquals(asError(majorLower()), object.toHtml(majorLower()));
    }

    @Test
    public void testIsDisconnect() {
        assertFalse(monitor.isDisconnect());
        monitor.setDisconnect(true);
        assertTrue(monitor.isDisconnect());
    }

    @Test
    public void testSetDisconnect() {
        monitor.setDisconnect(true);
        assertTrue(monitor.isDisconnect());
        monitor.setDisconnect(false);
        assertFalse(monitor.isDisconnect());
    }

    @Test
    public void testReadResolve() {
        monitor.setDisconnect(true);
        monitor.readResolve();
        assertTrue(monitor.isDisconnect());
        monitor.setDisconnect(false);
        monitor.readResolve();
        assertFalse(monitor.isDisconnect());
    }

    @Test
    public void testDoFillComparisonModeItems() {
        // Create an instance of JvmVersionDescriptor
        JVMVersionMonitor.JvmVersionDescriptor descriptor = new JVMVersionMonitor.JvmVersionDescriptor();

        // Call the method to test
        ListBoxModel items = descriptor.doFillComparisonModeItems();

        // Verify the items are not null
        assertNotNull(items, "The ListBoxModel should not be null");

        // Check the size of items
        assertEquals(
                JVMVersionComparator.ComparisonMode.values().length,
                items.size(),
                "The number of items should match the number of ComparisonMode values");

        // Verify each item
        for (JVMVersionComparator.ComparisonMode mode : JVMVersionComparator.ComparisonMode.values()) {
            assertEquals(
                    mode.getDescription(),
                    items.get(mode.ordinal()).name,
                    "The item description should match the mode's description");
            assertEquals(mode.name(), items.get(mode.ordinal()).value, "The item value should match the mode's name");
        }
    }
    @Test
    public void testReadResolveWithDisconnect() {
        // Create an instance of JVMVersionMonitor with no arguments
        JVMVersionMonitor monitor = new JVMVersionMonitor();

        // Set the disconnect field via the setter method
        monitor.setDisconnect(true);

        // Call readResolve to simulate deserialization logic
        Object result = monitor.readResolve();

        // Assert that the result is the same object
        assertEquals(monitor, result);

        // Verify that the isIgnored() method returns false (since disconnect was set to true)
        assertFalse(monitor.isIgnored());
    }

    @Test
    public void testReadResolveWithoutDisconnect() {
        // Create an instance of JVMVersionMonitor with no arguments
        JVMVersionMonitor monitor = new JVMVersionMonitor();

        // Call readResolve to simulate deserialization logic
        Object result = monitor.readResolve();

        // Assert that the result is the same object (the monitor instance itself)
        assertEquals(monitor, result);

        // Since disconnect is null, isIgnored() should return false (default behavior)
        assertFalse(monitor.isIgnored());
    }

    private String majorVersionMatch() {
        return Runtime.version().feature() + ".99.99.99+99";
    }

    private String majorLower() {
        return "1.99.99.99+99";
    }

    private String majorGreater() {
        return "999.99.99.99+99";
    }

    private String asError(String version) {
        return "<span class=error style='display:inline-block'>" + version + "</span>";
    }
}
