package hudson.plugin.versioncolumn;

import static hudson.plugin.versioncolumn.JVMVersionComparator.ComparisonMode.EXACT_MATCH;
import static hudson.plugin.versioncolumn.JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH;
import static hudson.plugin.versioncolumn.JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class JVMVersionComparatorTest {

    private static Object[] parameters() {
        return new Object[][] {
            {
                "17.0.12", "17.0.12", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.12", "17.0.12.1", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.12.1", "17.0.12", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.12.1", "17.0.12.1", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.13+8", "17.0.13+7", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.13+7", "17.0.13+8", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "21.0.4", "21.0.4", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "21.0.4", "21.0.4.1", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "21.0.4.1", "21.0.4", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "21.0.4.1", "21.0.4.1", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "21.0.5+8", "21.0.5+7", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "21.0.5+7", "21.0.5+8", RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE, true,
            },
            {
                "17.0.12", "17.0.12", MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.12", "17.0.12.1", MAJOR_MINOR_MATCH, false,
            },
            {
                "17.0.12.1", "17.0.12", MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.12.1", "17.0.12.1", MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.13+8", "17.0.13+7", MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.13+7", "17.0.13+8", MAJOR_MINOR_MATCH, true,
            },
            {
                "21.0.4", "21.0.4", MAJOR_MINOR_MATCH, true,
            },
            {
                "21.0.4", "21.0.4.1", MAJOR_MINOR_MATCH, false,
            },
            {
                "21.0.4.1", "21.0.4", MAJOR_MINOR_MATCH, true,
            },
            {
                "21.0.4.1", "21.0.4.1", MAJOR_MINOR_MATCH, true,
            },
            {
                "21.0.5+8", "21.0.5+7", MAJOR_MINOR_MATCH, true,
            },
            {
                "21.0.5+7", "21.0.5+8", MAJOR_MINOR_MATCH, true,
            },
            {
                "17.0.12", "17.0.12", EXACT_MATCH, true,
            },
            {
                "17.0.12", "17.0.12.1", EXACT_MATCH, false,
            },
            {
                "17.0.12.1", "17.0.12", EXACT_MATCH, false,
            },
            {
                "17.0.12.1", "17.0.12.1", EXACT_MATCH, true,
            },
            {
                "17.0.13+8", "17.0.13+7", EXACT_MATCH, true,
            },
            {
                "17.0.13+7", "17.0.13+8", EXACT_MATCH, true,
            },
            {
                "21.0.4", "21.0.4", EXACT_MATCH, true,
            },
            {
                "21.0.4", "21.0.4.1", EXACT_MATCH, false,
            },
            {
                "21.0.4.1", "21.0.4", EXACT_MATCH, false,
            },
            {
                "21.0.4.1", "21.0.4.1", EXACT_MATCH, true,
            },
            {
                "21.0.5+8", "21.0.5+7", EXACT_MATCH, true,
            },
            {
                "21.0.5+7", "21.0.5+8", EXACT_MATCH, true,
            },
        };
    }

    @Test
    public void testGetDescription() {
        JVMVersionComparator.ComparisonMode object = MAJOR_MINOR_MATCH;
        assertEquals(Messages.JVMVersionMonitor_MAJOR_MINOR_MATCH(), object.getDescription());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void smokes(
            String agentVersion,
            String controllerVersion,
            JVMVersionComparator.ComparisonMode comparisonMode,
            boolean isCompatible) {
        assertEquals(
                isCompatible,
                new JVMVersionComparator(
                                Runtime.Version.parse(controllerVersion),
                                Runtime.Version.parse(agentVersion),
                                comparisonMode)
                        .isCompatible());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void notSmokes(
            String agentVersion,
            String controllerVersion,
            JVMVersionComparator.ComparisonMode comparisonMode,
            boolean isCompatible) {
        assertEquals(
                !isCompatible,
                new JVMVersionComparator(
                                Runtime.Version.parse(controllerVersion),
                                Runtime.Version.parse(agentVersion),
                                comparisonMode)
                        .isNotCompatible());
    }
}
