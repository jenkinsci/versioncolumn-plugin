package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JVMVersionMonitorTest {

    private static Object[] parameters() {
        return new Object[][] {
            {
                JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, false,
            },
            {
                JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                JVMVersionComparator.ComparisonMode.EXACT_MATCH, true,
            },
            {
                JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, false,
            },
        };
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void checkDisconnect(JVMVersionComparator.ComparisonMode comparisonMode, boolean disconnect) {
        JVMVersionMonitor object = new JVMVersionMonitor(comparisonMode, disconnect);
        assertEquals(disconnect, object.isDisconnect());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void checkComparisonMode(JVMVersionComparator.ComparisonMode comparisonMode, boolean disconnect) {
        JVMVersionMonitor object = new JVMVersionMonitor(comparisonMode, disconnect);
        assertEquals(comparisonMode, object.getComparisonMode());
    }

    @Test
    public void checkToHtmlRendering() throws Exception {

        JVMVersionMonitor object = new JVMVersionMonitor(JVMVersionComparator.ComparisonMode.EXACT_MATCH, false);

        // N/A
        assertEquals("N/A", object.toHtml(null));
        assertEquals("N/A", object.toHtml("N/A"));

        // EXACT_MATCH
        assertEquals(
                Runtime.version().toString(), object.toHtml(Runtime.version().toString()));
        assertEquals(asError("1.1.1.1+1"), object.toHtml("1.1.1.1+1"));

        // RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE
        object = new JVMVersionMonitor(
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, false);
        assertEquals(
                Runtime.version().toString(), object.toHtml(Runtime.version().toString()));
        assertEquals(majorGreater(), object.toHtml(majorGreater()));
        assertEquals(majorVersionMatch(), object.toHtml(majorVersionMatch()));
        assertEquals(asError(majorLower()), object.toHtml(majorLower()));

        // MAJOR_MINOR_MATCH
        object = new JVMVersionMonitor(JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, false);
        assertEquals(
                Runtime.version().toString(), object.toHtml(Runtime.version().toString()));
        assertEquals(majorGreater(), object.toHtml(majorGreater()));
        assertEquals(majorVersionMatch(), object.toHtml(majorVersionMatch()));
        assertEquals(asError(majorLower()), object.toHtml(majorLower()));
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
