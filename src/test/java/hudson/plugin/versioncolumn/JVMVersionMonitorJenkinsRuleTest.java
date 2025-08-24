package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.Computer;
import hudson.slaves.DumbSlave;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JVMVersionMonitorJenkinsRuleTest {
    private JenkinsRule j;

    private JVMVersionMonitor monitor;
    private JVMVersionMonitor.JvmVersionDescriptor descriptor;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        monitor = new JVMVersionMonitor();
        descriptor = (JVMVersionMonitor.JvmVersionDescriptor) monitor.getDescriptor();
    }

    @Test
    void testMonitorWithNoAgent() throws Exception {
        Map<Computer, String> result = descriptor.monitor();
        Computer agentComputer = j.jenkins.getComputers()[0];
        assertTrue(result.containsKey(agentComputer), "Missing " + agentComputer);
        assertNotNull(result.get(agentComputer), "Null result for " + agentComputer);
        assertEquals(1, result.size(), "result is " + result.keySet());
    }

    @Test
    void testMonitorWithAgent() throws Exception {
        String firstResult = null;
        boolean waitForAgentToConnect = true;
        DumbSlave agent = j.createSlave(waitForAgentToConnect);
        Map<Computer, String> result = descriptor.monitor();
        for (Computer agentComputer : j.jenkins.getComputers()) {
            assertTrue(result.containsKey(agentComputer), "Missing " + agentComputer);
            assertNotNull(result.get(agentComputer), "Null result for " + agentComputer);
            if (firstResult == null) {
                firstResult = result.get(agentComputer);
            }
            // Same Java config for controller and agent should return same result
            assertEquals(
                    firstResult,
                    result.get(agentComputer),
                    "Mismatched result from %s : %s".formatted(agentComputer, result.get(agentComputer)));
        }
        assertEquals(2, result.size(), "result is " + result.keySet());
    }
}
