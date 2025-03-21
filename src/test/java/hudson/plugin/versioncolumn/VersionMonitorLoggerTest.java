package hudson.plugin.versioncolumn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

public class VersionMonitorLoggerTest {

    private VersionMonitor.DescriptorImpl descriptor;
    private Computer computer;
    private VirtualChannel channel;
    private Logger logger;
    private TestLogHandler handler;

    @Before
    public void setUp() {
        descriptor = spy(new VersionMonitor.DescriptorImpl());
        doReturn(false).when(descriptor).isIgnored(); // Not ignored

        computer = mock(Computer.class);
        channel = mock(VirtualChannel.class);

        // Set up logger to capture log messages
        logger = Logger.getLogger(VersionMonitor.class.getName());
        handler = new TestLogHandler();
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    @After
    public void tearDown() {
        logger.removeHandler(handler);
    }

    @Test
    public void testLoggingWhenMarkingOffline() throws IOException, InterruptedException {
        when(computer.getChannel()).thenReturn(channel);
        when(computer.getName()).thenReturn("TestAgent");
        when(channel.call(ArgumentMatchers.<MasterToSlaveCallable<String, IOException>>any()))
                .thenReturn("different-version");

        descriptor.monitor(computer);

        // Verify the log message contains the agent name
        assertTrue("Log should contain agent name", handler.getMessage().contains("TestAgent"));
        assertEquals(Level.WARNING, handler.getLevel());
    }

    // Custom log handler to capture logs
    private static class TestLogHandler extends Handler {
        private LogRecord lastRecord;

        @Override
        public void publish(LogRecord record) {
            lastRecord = record;
        }

        @Override
        public void flush() {
            // Not needed
        }

        @Override
        public void close() throws SecurityException {
            // Not needed
        }

        public String getMessage() {
            return lastRecord != null ? lastRecord.getMessage() : null;
        }

        public Level getLevel() {
            return lastRecord != null ? lastRecord.getLevel() : null;
        }
    }
}
