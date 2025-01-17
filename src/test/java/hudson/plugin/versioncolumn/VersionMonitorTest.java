package hudson.plugin.versioncolumn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeLauncher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.PretendSlave;
import org.mockito.ArgumentMatchers;

public class VersionMonitorTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    private VersionMonitor versionMonitor;
    private VersionMonitor.DescriptorImpl descriptor;

    @Before
    public void createVersionMonitor() {
        versionMonitor = new VersionMonitor();
        descriptor = (VersionMonitor.DescriptorImpl) versionMonitor.getDescriptor();
    }

    @Test
    public void testToHtml_NullVersion() {
        assertEquals("N/A", versionMonitor.toHtml(null));
    }

    @Test
    public void testToHtml_SameVersion() {
        String remotingVersion = RemotingVersionInfo.getEmbeddedVersion().toString();
        assertEquals(Launcher.VERSION, remotingVersion);
        assertEquals(Launcher.VERSION, versionMonitor.toHtml(remotingVersion));
    }

    @Test
    public void testToHtml_DifferentVersion() {
        String version = RemotingVersionInfo.getMinimumSupportedVersion().toString();
        assertEquals(Util.wrapToErrorSpan(version), versionMonitor.toHtml(version));
    }

    @Test
    public void testDescriptorImplConstructor() {
        VersionMonitor.DescriptorImpl descriptorImpl = new VersionMonitor.DescriptorImpl();
        assertSame("DESCRIPTOR should be set to this instance", VersionMonitor.DESCRIPTOR, descriptorImpl);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Remoting Version", descriptor.getDisplayName());
    }

    @Test
    public void testMonitor_NullChannel() throws Exception {
        PretendSlave pretendAgent = j.createPretendSlave(new TestLauncher());
        Computer computer = pretendAgent.createComputer();
        assertNull(computer.getChannel()); // Pre-condition for next assertion
        assertEquals("unknown-version", descriptor.monitor(computer));
    }

    @Test
    public void testMonitor_SameVersion() throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        Computer computer = agent.getComputer();
        assertNotNull(computer.getChannel());
        assertEquals(Launcher.VERSION, descriptor.monitor(computer));
    }

    @Test
    public void testMonitor_DifferentVersion_Ignored() throws IOException, InterruptedException {
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
    public void testMonitor_VersionIsNull_Ignored() throws IOException, InterruptedException {
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
    public void testMonitor_OfflineDueToMismatch_VersionsMatch() throws IOException, InterruptedException {
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
    public void testMonitor_OfflineDueToOtherCause() throws IOException, InterruptedException {
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
    public void testRemotingVersionMismatchCause() {
        String message = "Version mismatch";
        VersionMonitor.RemotingVersionMismatchCause cause = new VersionMonitor.RemotingVersionMismatchCause(message);

        assertEquals(message, cause.toString());
        assertEquals(VersionMonitor.class, cause.getTrigger());
    }

    private class TestLauncher implements FakeLauncher {

        @Override
        public Proc onLaunch(hudson.Launcher.ProcStarter p) throws IOException {
            throw new UnsupportedOperationException("Unsupported run.");
        }
    }
}
