package hudson.plugin.versioncolumn;

import io.jenkins.lib.versionnumber.JavaSpecificationVersion;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.Issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JVMVersionComparatorTest {

    @Test
    public void computeMajorMinor() {
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.0"));
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.0_66"));
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.1-blah_whatever$wat"));
    }

    private static Object[] parametersForCompatible() {
        return new Object[][]{
                {"1.8.0", "1.8.0", true},
                {"11.0.11", "11.0.11", true},
                {"1.8.0", "1.8.0", false},
                {"1.6.0", "1.6.0", true},
                {"1.6.0", "1.6.1_ublah_whatever", false},
                {"1.8.066", "1.8.0110", false},
        };
    }

    private static Object[] parametersForIncompatible() {
        return new Object[][]{
                {"1.5.0", "1.5.1", true},
                {"1.7.0", "1.6.0", true},
                {"1.7.0", "11.0.2", true},
                {"17.0.3", "11.0.2", true},
                {"1.8.0_66", "1.8.0_110", true},
                {"1.6.0", "1.6.1", true},
        };
    }

    @ParameterizedTest
    @MethodSource("parametersForCompatible")
    public void compatible(String masterVersion, String agentVersion, boolean exactMatch) {
        JVMVersionComparator.ComparisonMode comparisonMode = toExactMatch(exactMatch);
        assertTrue(new JVMVersionComparator(masterVersion, agentVersion, comparisonMode).isCompatible());
    }

    @ParameterizedTest
    @MethodSource("parametersForIncompatible")
    public void incompatible(String masterVersion, String agentVersion, boolean exactMatch) {
        JVMVersionComparator.ComparisonMode comparisonMode = toExactMatch(exactMatch);
        assertTrue(new JVMVersionComparator(masterVersion, agentVersion, comparisonMode).isNotCompatible());
    }

    private JVMVersionComparator.ComparisonMode toExactMatch(boolean exactMatch) {
        if (exactMatch) {
            return JVMVersionComparator.ComparisonMode.EXACT_MATCH;
        } else {
            return JVMVersionComparator.ComparisonMode.MAJOR_MINOR_MATCH;
        }
    }

    private static Object[] parametersForCompatibleBytecodeLevel() {
        return new Object[][]{
                {"1.8.0", JavaSpecificationVersion.JAVA_8.toClassVersion()},
                {"1.8.0", JavaSpecificationVersion.JAVA_7.toClassVersion()},
                {"1.8.0", JavaSpecificationVersion.JAVA_6.toReleaseVersion()},
                {"11.0.1", JavaSpecificationVersion.JAVA_11.toReleaseVersion()},
        };
    }

    private static Object[] parametersForIncompatibleBytecodeLevel() {
        return new Object[][]{
                {"1.7.0", JavaSpecificationVersion.JAVA_8.toClassVersion()},
                {"1.6.1", JavaSpecificationVersion.JAVA_7.toClassVersion()},
                {"1.5.3", JavaSpecificationVersion.JAVA_6.toClassVersion()},
                {"11.0.3", JavaSpecificationVersion.JAVA_12.toClassVersion()},
        };
    }


    @ParameterizedTest
    @MethodSource("parametersForCompatibleBytecodeLevel")
    public void compatibleBytecodeLevel(String agentVMVersion, final int masterBytecodeMajorVersion) {
        assertTrue(new JVMVersionComparator("whatever", agentVMVersion,
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE,
                new JVMVersionComparator.MasterBytecodeMajorVersionNumberGetter() {
                    @Override
                    public int get() {
                        return masterBytecodeMajorVersion;
                    }
                }).isCompatible());
    }

    @ParameterizedTest
    @MethodSource("parametersForIncompatibleBytecodeLevel")
    public void incompatibleBytecodeLevel(String agentVMVersion, final int masterBytecodeMajorVersion) {
        assertTrue(new JVMVersionComparator("whatever", agentVMVersion,
                JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE,
                new JVMVersionComparator.MasterBytecodeMajorVersionNumberGetter() {
                    @Override
                    public int get() {
                        return masterBytecodeMajorVersion;
                    }
                }).isNotCompatible());
    }

    @Issue("JENKINS-53445")
    @Test
    public void shouldNotThrowNPEWhenJVMVersionIsNotRecognized() {
        JVMVersionComparator jvmVersionComparator =
                new JVMVersionComparator("99.9", "99.9",
                        JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE);
        assertNotNull(jvmVersionComparator);
        assertFalse(jvmVersionComparator.isCompatible());
    }
}
