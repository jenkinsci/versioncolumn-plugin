package hudson.plugin.versioncolumn;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import hudson.Util;
import hudson.model.Computer;
import hudson.remoting.Launcher;
import hudson.remoting.VirtualChannel;
import hudson.slaves.OfflineCause;
import java.io.IOException;
import jenkins.security.MasterToSlaveCallable;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

public class VersionMonitorTest {

    @Test
    public void testConstructor() {
        VersionMonitor versionMonitor = new VersionMonitor();
        assertNotNull("VersionMonitor instance should not be null", versionMonitor);
    }

    @Test
    public void testToHtml_NullVersion() {
        VersionMonitor versionMonitor = new VersionMonitor();
        String result = versionMonitor.toHtml(null);
        assertEquals("N/A", result);
    }

    @Test
    public void testToHtml_SameVersion() {
        VersionMonitor versionMonitor = new VersionMonitor();
        String version = Launcher.VERSION;
        String result = versionMonitor.toHtml(version);
        assertEquals(version, result);
    }

    @Test
    public void testToHtml_DifferentVersion() {
        VersionMonitor versionMonitor = new VersionMonitor();
        String version = "different-version";
        String result = versionMonitor.toHtml(version);
        String expected = Util.wrapToErrorSpan(version);
        assertEquals(expected, result);
    }

    @Test
    public void testDescriptorImplConstructor() {
        VersionMonitor.DescriptorImpl descriptor = new VersionMonitor.DescriptorImpl();
        assertNotNull("DescriptorImpl instance should not be null", descriptor);
        assertSame("DESCRIPTOR should be set to this instance", VersionMonitor.DESCRIPTOR, descriptor);
    }

    @Test
    public void testGetDisplayName() {
        VersionMonitor.DescriptorImpl descriptor = new VersionMonitor.DescriptorImpl();
        String displayName = descriptor.getDisplayName();
        assertNotNull("Display name should not be null", displayName);
    }

    @Test
    public void testMonitor_NullChannel() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = new VersionMonitor.DescriptorImpl();
        Computer computer = mock(Computer.class);

        when(computer.getChannel()).thenReturn(null);

        String result = descriptor.monitor(computer);

        assertEquals("unknown-version", result);
        verify(computer, never()).setTemporarilyOffline(anyBoolean(), any());
    }

    @Test
    public void testMonitor_SameVersion() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = new VersionMonitor.DescriptorImpl();
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(Launcher.VERSION);
        when(computer.isOffline()).thenReturn(false);

        String result = descriptor.monitor(computer);

        assertEquals(Launcher.VERSION, result);
        verify(computer, never()).setTemporarilyOffline(anyBoolean(), any());
    }

    @Test
    public void testMonitor_DifferentVersion_NotIgnored() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(false).when(descriptor).isIgnored(); // Ensure isIgnored returns false

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        String differentVersion = "different-version";

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(differentVersion);
        when(computer.isOffline()).thenReturn(false);
        when(computer.getName()).thenReturn("TestComputer");

        String result = descriptor.monitor(computer);

        assertEquals(differentVersion, result);
        verify(computer).setTemporarilyOffline(eq(true), any(VersionMonitor.RemotingVersionMismatchCause.class));
    }

    @Test
    public void testMonitor_DifferentVersion_Ignored() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(true).when(descriptor).isIgnored(); // Ensure isIgnored returns true.

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        String differentVersion = "different-version";

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(differentVersion);
        when(computer.isOffline()).thenReturn(false);

        String result = descriptor.monitor(computer);

        assertEquals(differentVersion, result);
        verify(computer, never()).setTemporarilyOffline(anyBoolean(), any());
    }

    @Test
    public void testMonitor_VersionIsNull_NotIgnored() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(false).when(descriptor).isIgnored(); // Ensure isIgnored returns false

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(null);
        when(computer.isOffline()).thenReturn(false);
        when(computer.getName()).thenReturn("TestComputer");

        String result = descriptor.monitor(computer);

        assertNull(result);
        verify(computer).setTemporarilyOffline(eq(true), any(VersionMonitor.RemotingVersionMismatchCause.class));
    }

    @Test
    public void testMonitor_VersionIsNull_Ignored() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(true).when(descriptor).isIgnored(); // Ensure isIgnored returns true.

        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);
        VersionMonitor.RemotingVersionMismatchCause cause = mock(VersionMonitor.RemotingVersionMismatchCause.class);

        when(computer.getChannel()).thenReturn(channel);
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn(null);
        when(computer.isOffline()).thenReturn(true);
        when(computer.getOfflineCause()).thenReturn(cause);

        String result = descriptor.monitor(computer);

        assertNull(result);
        verify(computer).setTemporarilyOffline(eq(false), isNull());
    }

    @Test
    public void testMonitor_OfflineDueToMismatch_VersionsMatch() throws IOException, InterruptedException {
        VersionMonitor.DescriptorImpl descriptor = new VersionMonitor.DescriptorImpl();
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
        VersionMonitor.DescriptorImpl descriptor = new VersionMonitor.DescriptorImpl();
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

    // Since SlaveVersion is private, we cannot test it directly.
    // However, we can test its behavior indirectly through the monitor method.
}
