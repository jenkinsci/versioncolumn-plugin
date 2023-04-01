package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.*;

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
}
