package hudson.plugin.versioncolumn;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JVMVersionComparatorTest {

    private static Object[] parameters() {
        return new Object[][] {
            {
                "11.0.16", "11.0.17", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "11.0.17", "11.0.16", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "11.0.17", "11.0.17", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.4", "11.0.17", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "11.0.17", "17.0.4", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, false,
            },
            {
                "17.0.4", "17.0.4", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.4", "17.0.4.1", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.4.1", "17.0.4", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.4.1", "17.0.4.1", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.5+8", "17.0.5+7", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.5+7", "17.0.5+8", JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "11.0.16", "11.0.17", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, false,
            },
            {
                "11.0.17", "11.0.16", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "11.0.17", "11.0.17", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.4", "11.0.17", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "11.0.17", "17.0.4", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, false,
            },
            {
                "17.0.4", "17.0.4", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.4", "17.0.4.1", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, false,
            },
            {
                "17.0.4.1", "17.0.4", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.4.1", "17.0.4.1", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.5+8", "17.0.5+7", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.5+7", "17.0.5+8", JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH, true,
            },
            {
                "11.0.16", "11.0.17", JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                "11.0.17", "11.0.16", JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                "11.0.17", "11.0.17", JVMVersionComparator.ComparisonMode.EXACT_MATCH, true,
            },
            {
                "17.0.4", "11.0.17", JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                "11.0.17", "17.0.4", JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                "17.0.4", "17.0.4", JVMVersionComparator.ComparisonMode.EXACT_MATCH, true,
            },
            {
                "17.0.4", "17.0.4.1", JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                "17.0.4.1", "17.0.4", JVMVersionComparator.ComparisonMode.EXACT_MATCH, false,
            },
            {
                "17.0.4.1", "17.0.4.1", JVMVersionComparator.ComparisonMode.EXACT_MATCH, true,
            },
            {
                "17.0.5+8", "17.0.5+7", JVMVersionComparator.ComparisonMode.EXACT_MATCH, true,
            },
            {
                "17.0.5+7", "17.0.5+8", JVMVersionComparator.ComparisonMode.EXACT_MATCH, true,
            },
        };
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void smokes(String agentVersion, String controllerVersion, JVMVersionComparator.ComparisonMode comparisonMode, boolean isCompatible) {
        assertEquals(isCompatible, new JVMVersionComparator(Runtime.Version.parse(controllerVersion), Runtime.Version.parse(agentVersion), comparisonMode).isCompatible());
    }
}
