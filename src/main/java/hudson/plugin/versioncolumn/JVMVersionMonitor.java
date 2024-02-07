/*
 * The MIT License
 *
 * Copyright (c) 2017-, Baptiste Mathus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugin.versioncolumn;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.ComputerSet;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.MonitorOfflineCause;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Callable;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.Exported;

public class JVMVersionMonitor extends NodeMonitor {

    private static final Runtime.Version CONTROLLER_VERSION = Runtime.version();
    private static final Logger LOGGER = Logger.getLogger(JVMVersionMonitor.class.getName());

    private JVMVersionComparator.ComparisonMode comparisonMode =
            JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE;
    private transient Boolean disconnect;

    @DataBoundConstructor
    public JVMVersionMonitor(JVMVersionComparator.ComparisonMode comparisonMode) {
        this.comparisonMode = comparisonMode;
    }

    public JVMVersionMonitor() {}

    @SuppressWarnings("unused") // jelly
    public boolean isDisconnect() {
        return !isIgnored();
    }

    // should be restricted/deprecated but that breaks casc
    @DataBoundSetter
    public void setDisconnect(boolean disconnect) {
        setIgnored(!disconnect);
    }

    public Object readResolve() {
        if (disconnect != null) {
            this.setIgnored(!disconnect);
        }
        return this;
    }

    @SuppressWarnings("unused") // jelly
    public String toHtml(String version) {
        if (version == null || version.equals("N/A")) {
            return "N/A";
        }
        final JVMVersionComparator jvmVersionComparator =
                new JVMVersionComparator(CONTROLLER_VERSION, Runtime.Version.parse(version), comparisonMode);
        if (jvmVersionComparator.isNotCompatible()) {
            return Util.wrapToErrorSpan(version);
        }
        return version;
    }

    public JVMVersionComparator.ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    @Extension
    @Symbol("jvmVersion")
    public static class JvmVersionDescriptor extends AbstractAsyncNodeMonitorDescriptor<String> {

        @Override
        protected Map<Computer, String> monitor() throws InterruptedException {
            Result<String> base = monitorDetailed();
            Map<Computer, String> data = base.getMonitoringData();
            JVMVersionMonitor monitor =
                    (JVMVersionMonitor) ComputerSet.getMonitors().get(this);
            for (Map.Entry<Computer, String> e : data.entrySet()) {
                Computer computer = e.getKey();
                String version = e.getValue();
                if (base.getSkipped().contains(computer)) {
                    assert version == null;
                    continue;
                }
                if (version == null) {
                    e.setValue(version = get(computer));
                }
                markNodeOfflineOrOnline(computer, version, monitor);
            }
            return data;
        }

        private void markNodeOfflineOrOnline(Computer c, String agentVersionStr, JVMVersionMonitor monitor) {
            if (agentVersionStr == null) {
                return;
            }
            Runtime.Version agentVersion;
            try {
                agentVersion = Runtime.Version.parse(agentVersionStr);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Failed to parse agent version: " + agentVersionStr, e);
                return;
            }
            final JVMVersionComparator jvmVersionComparator =
                    new JVMVersionComparator(CONTROLLER_VERSION, agentVersion, monitor.comparisonMode);

            if (jvmVersionComparator.isNotCompatible()) {
                if (!isIgnored()) {
                    LOGGER.warning(
                            Messages.JVMVersionMonitor_MarkedOffline(c.getName(), CONTROLLER_VERSION, agentVersionStr));
                    markOffline(c, new JVMMismatchCause(Messages.JVMVersionMonitor_OfflineCause()));
                } else {
                    LOGGER.finer("Version incompatibility detected, but keeping the agent '"
                            + c.getName()
                            + "' online per the node monitor configuration");
                    if (c.isOffline() && c.getOfflineCause() instanceof JVMMismatchCause) {
                        c.setTemporarilyOffline(false, null);
                    }
                }
            } else {
                if (c.isOffline() && c.getOfflineCause() instanceof JVMMismatchCause) {
                    c.setTemporarilyOffline(false, null);
                }
            }
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.JVMVersionMonitor_DisplayName();
        }

        @Override
        protected Callable<String, IOException> createCallable(Computer c) {
            return new JavaVersion();
        }

        public ListBoxModel doFillComparisonModeItems() {
            ListBoxModel items = new ListBoxModel();
            for (JVMVersionComparator.ComparisonMode goal : JVMVersionComparator.ComparisonMode.values()) {
                items.add(goal.getDescription(), goal.name());
            }
            return items;
        }
    }

    public static class JVMMismatchCause extends MonitorOfflineCause {

        private final String message;

        public JVMMismatchCause(String message) {
            this.message = message;
        }

        @Override
        @Exported(name = "description")
        public String toString() {
            return message;
        }

        @NonNull
        @Override
        public Class<? extends NodeMonitor> getTrigger() {
            return JVMVersionMonitor.class;
        }
    }

    private static class JavaVersion extends MasterToSlaveCallable<String, IOException> {
        @Override
        public String call() {
            return Runtime.version().toString();
        }
    }
}
