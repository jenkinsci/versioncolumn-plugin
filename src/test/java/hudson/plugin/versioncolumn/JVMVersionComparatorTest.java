package hudson.plugin.versioncolumn;


import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class JVMVersionComparatorTest {

    @Test
    public void computeMajorMinor() {

        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.0"));
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.0_66"));
        assertEquals("1.8", JVMVersionComparator.computeMajorMinor("1.8.1-blah_whatever$wat"));
    }

    private Object[] parametersForCompatible() {
        return new Object[][] {
                {"1.8.0", "1.8.0", true },
                {"1.8.0", "1.8.0", false },
                {"1.6.0", "1.6.0", false},
                {"1.6.0", "1.6.1_ublah_whatever", false },
                {"1.8.066", "1.8.0110", false },
        };
    }

    private Object[] parametersForIncompatible() {
        return new Object[][] {
                {"1.5.0", "1.5.1", true },
                {"1.7.0", "1.6.0", true },
                {"1.8.0_66", "1.8.0_110", true },
                {"1.6.0", "1.6.1", true },
        };
    }
    @Test
    @Parameters
    public void compatible(String masterVersion, String agentVersion, boolean exact) {
        assertTrue(new JVMVersionComparator(masterVersion, agentVersion, exact).isCompatible());
    }

    @Test
    @Parameters
    public void incompatible(String masterVersion, String agentVersion, boolean exact) {
        assertTrue(new JVMVersionComparator(masterVersion, agentVersion, exact).isNotCompatible());
    }


}