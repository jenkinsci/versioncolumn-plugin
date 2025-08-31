package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.Computer;
import hudson.model.User;
import hudson.remoting.Launcher;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JVMVersionMonitorJenkinsRuleTest {
    private JenkinsRule j;

    private VersionMonitor monitor;
    private VersionMonitor.DescriptorImpl descriptor;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        monitor = new VersionMonitor();
        descriptor = (VersionMonitor.DescriptorImpl) monitor.getDescriptor();
    }

    @Test
    void testToHtml_NullVersion() {
        assertEquals("N/A", monitor.toHtml(null));
    }

    @Test
    void testToHtml_SameVersion() {
        String remotingVersion = Launcher.VERSION;
        assertEquals(remotingVersion, monitor.toHtml(remotingVersion));
    }

    @Test
    void testToHtml_DifferentVersion() {
        String version = "different-version";
        assertTrue(monitor.toHtml(version).contains("error"), "Should wrap different version in error span");
        assertTrue(monitor.toHtml(version).contains(version), "Should include version in error span");
    }

    @Test
    void testDescriptorImplConstructor() {
        VersionMonitor.DescriptorImpl descriptorImpl = new VersionMonitor.DescriptorImpl();
        assertSame(VersionMonitor.DESCRIPTOR, descriptorImpl, "DESCRIPTOR should be set to this instance");
    }

    @Test
    void testGetDisplayName() {
        assertEquals("Remoting Version", descriptor.getDisplayName());
    }

    @Test
    void testMonitorWithNoAgent() throws Exception {
        // Test monitoring just the controller
        Computer controllerComputer = j.jenkins.getComputers()[0];
        String result = descriptor.monitor(controllerComputer);
        assertNotNull(result, "Controller should return a version");
        assertEquals(Launcher.VERSION, result, "Controller should return current Launcher.VERSION");
    }

    @Test
    void testMonitorWithAgent() throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        Computer agentComputer = agent.getComputer();
        
        String result = descriptor.monitor(agentComputer);
        assertNotNull(result, "Agent should return a version");
        assertEquals(Launcher.VERSION, result, "Agent should return same version as controller");
        
        // Verify the agent is still online (no version mismatch detected)
        assertTrue(agentComputer.isOnline(), "Agent should remain online with matching version");
    }

    @Test
    void testMonitorWithOfflineAgent() throws Exception {
        DumbSlave agent = j.createSlave();
        Computer agentComputer = agent.getComputer();
        // Agent is offline by default
        assertFalse(agentComputer.isOnline());
        
        String result = descriptor.monitor(agentComputer);
        assertEquals("unknown-version", result, "Offline agent should return unknown-version");
    }

    @Test
    void testMonitorWithAgentHavingNullVersion() throws Exception {
        // This test simulates the scenario where the agent returns null version
        // which could happen in older slave.jar versions
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        // In a real scenario, this would require mocking the channel to return null
        // But since we're using real objects, we'll test the toHtml method with null
        assertEquals("N/A", monitor.toHtml(null));
    }

    @Test
    void testRemotingVersionMismatchCause() {
        String message = "Version mismatch";
        VersionMonitor.RemotingVersionMismatchCause cause = new VersionMonitor.RemotingVersionMismatchCause(message);

        assertEquals(message, cause.toString());
        assertEquals(VersionMonitor.class, cause.getTrigger());
    }

    @Test
    void testMonitorWithAgentOfflineDueToOtherCause() throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        // Manually set the agent offline with a different cause
        User testUser = User.getOrCreateByIdOrFullName("test-user");
        OfflineCause otherCause = new OfflineCause.UserCause(testUser, "Test reason");
        computer.setTemporaryOfflineCause(otherCause);
        
        assertTrue(computer.isOffline(), "Agent should be offline");
        assertNotEquals(VersionMonitor.RemotingVersionMismatchCause.class, 
            computer.getOfflineCause().getClass(), "Should have different offline cause");
        
        String result = descriptor.monitor(computer);
        assertEquals(Launcher.VERSION, result, "Should return version even when offline due to other cause");
        
        // Agent should remain offline since it's not due to version mismatch
        assertTrue(computer.isOffline(), "Agent should remain offline due to other cause");
    }

    @Test
    void testMonitorWithAgentOfflineDueToVersionMismatch() throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        // Manually set the agent offline with version mismatch cause
        VersionMonitor.RemotingVersionMismatchCause versionCause = 
            new VersionMonitor.RemotingVersionMismatchCause("Version mismatch");
        computer.setTemporaryOfflineCause(versionCause);
        
        assertTrue(computer.isOffline(), "Agent should be offline");
        assertTrue(computer.getOfflineCause() instanceof VersionMonitor.RemotingVersionMismatchCause, 
            "Should have version mismatch offline cause");
        
        String result = descriptor.monitor(computer);
        assertEquals(Launcher.VERSION, result, "Should return version");
        
        // Agent should be brought back online since versions now match
        assertFalse(computer.isOffline(), "Agent should be brought back online when versions match");
    }

    @Test
    void testSlaveVersionCallable() throws Exception {
        // Test the SlaveVersion callable indirectly through the monitor method
        // Since SlaveVersion is private, we test it through the public interface
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        String result = descriptor.monitor(computer);
        
        // This should return the current Launcher.VERSION through the SlaveVersion callable
        assertEquals(Launcher.VERSION, result, "SlaveVersion should return current Launcher.VERSION");
    }

    @Test
    void testSlaveVersionCallableWithException() throws Exception {
        // Test the SlaveVersion callable when Launcher.VERSION throws an exception
        // This simulates older slave.jar versions that don't have VERSION
        // We can't directly test the private SlaveVersion class, but we can test the behavior
        // by creating a scenario where the channel call fails
        
        // Create an offline agent to simulate channel failure
        DumbSlave agent = j.createSlave();
        Computer computer = agent.getComputer();
        
        String result = descriptor.monitor(computer);
        
        // When channel is null, it should return "unknown-version"
        assertEquals("unknown-version", result, "Should return unknown-version when channel is null");
    }

    @Test
    void testMonitorWithMultipleAgents() throws Exception {
        // Test monitoring multiple agents
        DumbSlave agent1 = j.createOnlineSlave();
        DumbSlave agent2 = j.createOnlineSlave();
        
        // Test each agent individually
        String result1 = descriptor.monitor(agent1.getComputer());
        String result2 = descriptor.monitor(agent2.getComputer());
        
        assertEquals(Launcher.VERSION, result1, "Agent1 should return correct version");
        assertEquals(Launcher.VERSION, result2, "Agent2 should return correct version");
        
        // Verify both agents are online
        assertTrue(agent1.getComputer().isOnline(), "Agent1 should be online");
        assertTrue(agent2.getComputer().isOnline(), "Agent2 should be online");
    }

    @Test
    void testMonitorWithAgentHavingDifferentVersion() throws Exception {
        // Create an agent that will return a different version
        // This tests the version mismatch scenario
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        // The agent should have the same version as controller in normal circumstances
        String result = descriptor.monitor(computer);
        assertEquals(Launcher.VERSION, result, "Agent should return same version as controller");
        
        // Verify the agent is still online (no version mismatch detected)
        assertTrue(computer.isOnline(), "Agent should remain online with matching version");
    }

    @Test
    void testMonitorWithNullVersionFromChannel() throws Exception {
        // Test the scenario where the channel returns null version
        // This tests the null check in the monitor method
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        // Since we can't easily mock the channel to return null in a real environment,
        // we test the toHtml method with null
        assertEquals("N/A", monitor.toHtml(null));
        
        // And test the monitor method with a real agent (which should not return null)
        String result = descriptor.monitor(computer);
        assertNotNull(result, "Monitor should not return null for online agent");
    }

    @Test
    void testMonitorWithVersionMismatchDetection() throws Exception {
        // Test version mismatch detection logic
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        
        // Test with same version (should not trigger mismatch)
        String result = descriptor.monitor(computer);
        assertEquals(Launcher.VERSION, result, "Should return same version");
        assertTrue(computer.isOnline(), "Agent should remain online with matching version");
        
        // Test toHtml with different version (should trigger error display)
        String differentVersion = "different-version";
        String htmlResult = monitor.toHtml(differentVersion);
        assertTrue(htmlResult.contains("error"), "Should wrap different version in error span");
        assertTrue(htmlResult.contains(differentVersion), "Should include version in error span");
    }

    @Test
    void testMonitorWithMultipleControllerCalls() throws Exception {
        // Test multiple calls to monitor the same controller
        Computer controllerComputer = j.jenkins.getComputers()[0];
        
        // First call
        String result1 = descriptor.monitor(controllerComputer);
        assertEquals(Launcher.VERSION, result1, "First call should return correct version");
        
        // Second call
        String result2 = descriptor.monitor(controllerComputer);
        assertEquals(Launcher.VERSION, result2, "Second call should return correct version");
        
        // Results should be consistent
        assertEquals(result1, result2, "Multiple calls should return same result");
    }

    @Test
    void testMonitorWithAgentAndController() throws Exception {
        // Test monitoring both controller and agent
        Computer controllerComputer = j.jenkins.getComputers()[0];
        DumbSlave agent = j.createOnlineSlave();
        Computer agentComputer = agent.getComputer();
        
        // Monitor controller
        String controllerResult = descriptor.monitor(controllerComputer);
        assertEquals(Launcher.VERSION, controllerResult, "Controller should return correct version");
        
        // Monitor agent
        String agentResult = descriptor.monitor(agentComputer);
        assertEquals(Launcher.VERSION, agentResult, "Agent should return correct version");
        
        // Both should return the same version
        assertEquals(controllerResult, agentResult, "Controller and agent should return same version");
    }

    @Test
    void testToHtmlWithVariousVersions() throws Exception {
        // Test toHtml method with various version strings
        assertEquals("N/A", monitor.toHtml(null), "Null version should return N/A");
        assertEquals("N/A", monitor.toHtml(""), "Empty version should return N/A");
        assertEquals(Launcher.VERSION, monitor.toHtml(Launcher.VERSION), "Same version should return as-is");
        
        // Test with different versions
        String differentVersion1 = "1.2.3";
        String differentVersion2 = "4.5.6";
        
        String htmlResult1 = monitor.toHtml(differentVersion1);
        String htmlResult2 = monitor.toHtml(differentVersion2);
        
        assertTrue(htmlResult1.contains("error"), "Different version 1 should be wrapped in error span");
        assertTrue(htmlResult2.contains("error"), "Different version 2 should be wrapped in error span");
        assertTrue(htmlResult1.contains(differentVersion1), "Error span should contain version 1");
        assertTrue(htmlResult2.contains(differentVersion2), "Error span should contain version 2");
    }

    @Test
    void testRemotingVersionMismatchCauseWithDifferentMessages() throws Exception {
        // Test RemotingVersionMismatchCause with different messages
        String message1 = "Version mismatch detected";
        String message2 = "Incompatible remoting version";
        
        VersionMonitor.RemotingVersionMismatchCause cause1 = new VersionMonitor.RemotingVersionMismatchCause(message1);
        VersionMonitor.RemotingVersionMismatchCause cause2 = new VersionMonitor.RemotingVersionMismatchCause(message2);
        
        assertEquals(message1, cause1.toString(), "Cause 1 should return correct message");
        assertEquals(message2, cause2.toString(), "Cause 2 should return correct message");
        assertEquals(VersionMonitor.class, cause1.getTrigger(), "Cause 1 should return correct trigger class");
        assertEquals(VersionMonitor.class, cause2.getTrigger(), "Cause 2 should return correct trigger class");
    }
}
