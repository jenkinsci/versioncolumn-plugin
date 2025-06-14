package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import jenkins.security.MasterToSlaveCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentMatchers;

@WithJenkins
class VersionMonitorLoggerTest {

    private JenkinsRule j;
    private LoggerRule loggerRule = new LoggerRule();
    private VersionMonitor.DescriptorImpl descriptor;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;

        // Configure the LoggerRule to capture messages from VersionMonitor
        loggerRule.record(VersionMonitor.class, Level.WARNING).capture(10);

        // Create a spy of the descriptor instead of trying to set a private field
        descriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(false).when(descriptor).isIgnored();

        // Set the static DESCRIPTOR field to our spy
        VersionMonitor.DESCRIPTOR = descriptor;
    }

    @Test
    void testLoggingWhenMarkingOffline() throws IOException, InterruptedException {
        // Create mock computer and channel
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        // Set up the mocks with expected behavior
        when(computer.getChannel()).thenReturn(channel);
        when(computer.getName()).thenReturn("TestAgent");
        doReturn("different-version")
                .when(channel)
                .call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any());

        // Call monitor to trigger the logging
        descriptor.monitor(computer);

        // Assert that the log contains the agent name
        assertTrue(
                loggerRule.getMessages().stream().anyMatch(msg -> msg.contains("TestAgent")),
                "Log should contain agent name");

        // Verify the log level is WARNING
        assertEquals(
                Level.WARNING.getName(),
                loggerRule.getRecords().stream()
                        .filter(record -> record.getMessage().contains("TestAgent"))
                        .findFirst()
                        .map(LogRecord::getLevel)
                        .map(Level::getName)
                        .orElse(null));
    }

    @Test
    void testNoLoggingWhenIgnored() throws IOException, InterruptedException {
        // Create a new logger rule to reset messages
        loggerRule.record(VersionMonitor.class, Level.WARNING).capture(10);

        // Set descriptor to ignore version differences
        doReturn(true).when(descriptor).isIgnored();

        // Create mock computer and channel
        Computer computer = mock(Computer.class);
        VirtualChannel channel = mock(VirtualChannel.class);

        // Set up the mocks with expected behavior
        when(computer.getChannel()).thenReturn(channel);
        when(computer.getName()).thenReturn("TestAgent");
        doReturn("different-version")
                .when(channel)
                .call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any());

        // Call monitor
        descriptor.monitor(computer);

        // There should be no log messages about marking an agent offline
        // The log might include other messages, but not about marking offline
        assertTrue(
                loggerRule.getMessages().stream().noneMatch(msg -> msg.contains("offline")),
                "No messages about marking offline should be present");
    }
}
