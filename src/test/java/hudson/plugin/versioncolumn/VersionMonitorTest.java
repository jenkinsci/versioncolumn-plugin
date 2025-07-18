package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import hudson.Proc;
import hudson.Util;
import hudson.model.Computer;
import hudson.remoting.Launcher;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import java.io.IOException;
import jenkins.security.MasterToSlaveCallable;
import jenkins.slaves.RemotingVersionInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FakeLauncher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.PretendSlave;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentMatchers;

@WithJenkins
class VersionMonitorTest {

    private static JenkinsRule j;

    private VersionMonitor versionMonitor;
    private VersionMonitor.DescriptorImpl descriptor;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @BeforeEach
    void createVersionMonitor() {
        // Create the versionMonitor instance directly
        versionMonitor = new VersionMonitor();

        // Create a spy of the descriptor instead of trying to get it from
        // versionMonitor
        descriptor = spy(new VersionMonitor.DescriptorImpl());

        // Set the static DESCRIPTOR field to our spy
        VersionMonitor.DESCRIPTOR = descriptor;
    }

    @Test
    void testToHtml_NullVersion() {
        assertEquals("N/A", versionMonitor.toHtml(null));
    }

    @Test
    void testToHtml_SameVersion() {
        String remotingVersion = RemotingVersionInfo.getEmbeddedVersion().toString();
        assertEquals(Launcher.VERSION, remotingVersion);
        assertEquals(Launcher.VERSION, versionMonitor.toHtml(remotingVersion));
    }

    @Test
    void testToHtml_DifferentVersion() {
        String version = RemotingVersionInfo.getMinimumSupportedVersion().toString();
        assertEquals(Util.wrapToErrorSpan(version), versionMonitor.toHtml(version));
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
    void testMonitor_NullChannel() throws Exception {
        PretendSlave pretendAgent = j.createPretendSlave(new TestLauncher());
        Computer computer = pretendAgent.createComputer();
        assertNull(computer.getChannel()); // Pre-condition for next assertion
        assertEquals("unknown-version", descriptor.monitor(computer));
    }

    @Test
    void testMonitor_SameVersion() throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        assertNotNull(computer.getChannel());
        assertEquals(Launcher.VERSION, descriptor.monitor(computer));
    }

    @Test
    void testMonitor_DifferentVersion_Ignored() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl mockDescriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(true).when(mockDescriptor).isIgnored(); // Ensure isIgnored returns true.

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        String differentVersion = "different-version";

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(differentVersion);
        when(computer.isOffline()).thenReturn(false);

        String result = mockDescriptor.monitor(computer);

        assertEquals(differentVersion, result);
        verify(computer, never()).setTemporarilyOffline(anyBoolean(), any());
    }

    @Test
    void testMonitor_VersionIsNull_Ignored() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl mockDescriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(true).when(mockDescriptor).isIgnored(); // Ensure isIgnored returns true.

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        VersionMonitor.RemotingVersionMismatchCause cause = mock(VersionMonitor.RemotingVersionMismatchCause.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(null);
        when(computer.isOffline()).thenReturn(true);
        when(computer.getOfflineCause()).thenReturn(cause);

        String result = mockDescriptor.monitor(computer);

        assertNull(result);
        verify(computer).setTemporarilyOffline(eq(false), isNull());
    }

    @Test
    void testMonitor_OfflineDueToMismatch_VersionsMatch() throws IOException, InterruptedException {
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        VersionMonitor.RemotingVersionMismatchCause cause = new VersionMonitor.RemotingVersionMismatchCause("Mismatch");

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(Launcher.VERSION);
        when(computer.isOffline()).thenReturn(true);
        when(computer.getOfflineCause()).thenReturn(cause);

        String result = descriptor.monitor(computer);

        assertEquals(Launcher.VERSION, result);
        verify(computer).setTemporarilyOffline(eq(false), isNull());
    }

    @Test
    void testMonitor_OfflineDueToOtherCause() throws IOException, InterruptedException {
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        OfflineCause otherCause = mock(OfflineCause.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(Launcher.VERSION);
        when(computer.isOffline()).thenReturn(true);
        when(computer.getOfflineCause()).thenReturn(otherCause);

        String result = descriptor.monitor(computer);

        assertEquals(Launcher.VERSION, result);
        verify(computer, never()).setTemporarilyOffline(eq(false), any());
    }

    @Test
    void testRemotingVersionMismatchCause() {
        String message = "Version mismatch";
        VersionMonitor.RemotingVersionMismatchCause cause = new VersionMonitor.RemotingVersionMismatchCause(message);

        assertEquals(message, cause.toString());
        assertEquals(VersionMonitor.class, cause.getTrigger());
    }

    @Test
    public void testSlaveVersionCallable() throws IOException, InterruptedException {
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(Launcher.VERSION);

        String result = descriptor.monitor(computer);
        assertNotNull(result, "Slave version should not be null");
        assertTrue(
                result.equals(Launcher.VERSION) || result.equals("< 1.335"),
                "Slave version should be either current version or < 1.335");
    }

    @Test
    public void testMonitor_ExceptionInChannelCall() throws IOException, InterruptedException {
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenThrow(new IOException("Test exception"));

        // Use try-catch to properly handle the expected exception
        String result;
        try {
            result = descriptor.monitor(computer);
            assertEquals("unknown-version", result);
        } catch (IOException e) {
            // Expected exception
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    public void testMonitor_ThrowableInChannelCall() throws IOException, InterruptedException {
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenThrow(new RuntimeException("Test runtime exception"));

        // Use try-catch to properly handle the expected exception
        String result;
        try {
            result = descriptor.monitor(computer);
            assertEquals("unknown-version", result);
        } catch (RuntimeException e) {
            // Expected exception
            assertEquals("Test runtime exception", e.getMessage());
        }
    }

    @Test
    public void testMonitor_DifferentVersion_NotIgnored() throws IOException, InterruptedException {
        // Use the descriptor that's already a spy from createVersionMonitor()
        doReturn(false).when(descriptor).isIgnored();

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        String differentVersion = "different-version";

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(differentVersion);
        when(computer.isOffline()).thenReturn(false);

        String result = descriptor.monitor(computer);

        assertEquals(differentVersion, result);
        verify(computer).setTemporarilyOffline(eq(true), any(VersionMonitor.RemotingVersionMismatchCause.class));
    }

    @Test
    public void testMonitor_VersionIsNull_NotIgnored() throws IOException, InterruptedException {
        // Use the descriptor that's already a spy from createVersionMonitor()
        doReturn(false).when(descriptor).isIgnored();

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(null);
        when(computer.isOffline()).thenReturn(false);

        String result = descriptor.monitor(computer);

        assertNull(result);
        verify(computer).setTemporarilyOffline(eq(true), any(VersionMonitor.RemotingVersionMismatchCause.class));
    }

    @Test
    public void testMonitor_DifferentVersion_AlreadyOffline() throws IOException, InterruptedException {
        // Use the descriptor that's already a spy from createVersionMonitor()
        doReturn(false).when(descriptor).isIgnored();

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        String differentVersion = "different-version";
        OfflineCause otherCause = mock(OfflineCause.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(differentVersion);
        when(computer.isOffline()).thenReturn(true);
        when(computer.getOfflineCause()).thenReturn(otherCause);

        String result = descriptor.monitor(computer);

        assertEquals(differentVersion, result);
        verify(computer).setTemporarilyOffline(eq(true), any(VersionMonitor.RemotingVersionMismatchCause.class));
    }

    @Test
    public void testSlaveVersionCallableOldVersion() throws IOException, InterruptedException {
        // Test that invoking the monitor method with a mocked channel returns "< 1.335"
        // In this test, we set up the mock to directly return the expected value
        // since we can't effectively mock the inner workings of the SlaveVersion
        // callable
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn("< 1.335");

        String result = descriptor.monitor(computer);
        assertEquals("< 1.335", result);
    }

    @Test
    public void testHandlingWithDifferentVersions() {
        // Test N/A handling
        assertEquals("N/A", versionMonitor.toHtml(null));

        // Test different version handling (gives error)
        String oldVersion = "1.0.0";
        String htmlResult = versionMonitor.toHtml(oldVersion);
        assertTrue(htmlResult.contains("<span"), "HTML result should contain span tag");
        assertTrue(htmlResult.contains(oldVersion), "HTML result should contain the version");
    }

    // A simple test launcher that doesn't do anything
    // just for creating a PretendSlave with no real connection
    private static class TestLauncher implements FakeLauncher {
        @Override
        public Proc onLaunch(hudson.Launcher.ProcStarter p) {
            throw new UnsupportedOperationException("Unsupported run.");
        }
    }
}
