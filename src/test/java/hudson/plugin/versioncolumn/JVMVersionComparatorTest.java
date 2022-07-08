package hudson.plugin.versioncolumn;


import io.jenkins.lib.versionnumber.JavaSpecificationVersion;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class JVMVersionComparatorTest {

    @Test
    public void computeMajorMinor() {

        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.0"));
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.0_66"));
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.1-blah_whatever$wat"));
    }

    @SuppressWarnings("unused")
    private Object[] parametersForCompatible() {
        return new Object[][]{
                {"1.8.0", "1.8.0", true},
                {"11.0.11", "11.0.11", true},
                {"1.8.0", "1.8.0", false},
                {"1.6.0", "1.6.0", true},
                {"1.6.0", "1.6.1_ublah_whatever", false},
                {"1.8.066", "1.8.0110", false},
        };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForIncompatible() {
        return new Object[][]{
                {"1.5.0", "1.5.1", true},
                {"1.7.0", "1.6.0", true},
                {"1.7.0", "11.0.2", true},
                {"17.0.3", "11.0.2", true},
                {"1.8.0_66", "1.8.0_110", true},
                {"1.6.0", "1.6.1", true},
        };
    }

    @Test
    @Parameters
    public void compatible(String masterVersion, String agentVersion, boolean exactMatch) {
        JVMVersionComparator.ComparisonMode comparisonMode = toExactMatch(exactMatch);
        assertTrue(new JVMVersionComparator(masterVersion, agentVersion, comparisonMode).isCompatible());
    }

    @Test
    @Parameters
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

    @SuppressWarnings("unused")
    private Object[] parametersForCompatibleBytecodeLevel() {
        return new Object[][]{
                {"1.8.0", JavaSpecificationVersion.JAVA_8.toClassVersion()},
                {"1.8.0", JavaSpecificationVersion.JAVA_7.toClassVersion()},
                {"1.8.0", JavaSpecificationVersion.JAVA_6.toReleaseVersion()},
                {"11.0.1", JavaSpecificationVersion.JAVA_11.toReleaseVersion()},
        };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForIncompatibleBytecodeLevel() {
        return new Object[][]{
                {"1.7.0", JavaSpecificationVersion.JAVA_8.toClassVersion()},
                {"1.6.1", JavaSpecificationVersion.JAVA_7.toClassVersion()},
                {"1.5.3", JavaSpecificationVersion.JAVA_6.toClassVersion()},
                {"11.0.3", JavaSpecificationVersion.JAVA_12.toClassVersion()},
        };
    }


    @Test
    @Parameters
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

    @Test
    @Parameters
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
