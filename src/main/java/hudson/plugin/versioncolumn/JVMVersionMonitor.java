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
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Callable;
import hudson.slaves.OfflineCause;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.DataBoundConstructor;

public class JVMVersionMonitor extends NodeMonitor {

    private static final Runtime.Version CONTROLLER_VERSION = Runtime.version();
    private static final Logger LOGGER = Logger.getLogger(JVMVersionMonitor.class.getName());

    private JVMVersionComparator.ComparisonMode comparisonMode =
            JVMVersionComparator.ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE;
    private boolean disconnect = true;

    @DataBoundConstructor
    public JVMVersionMonitor(JVMVersionComparator.ComparisonMode comparisonMode, boolean disconnect) {
        this.comparisonMode = comparisonMode;
        this.disconnect = disconnect;
    }

    public JVMVersionMonitor() {}

    @SuppressWarnings("unused") // jelly
    public boolean isDisconnect() {
        return disconnect;
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

    @Override
    public Object data(Computer c) {

        String agentVersionStr = (String) super.data(c);
        if (agentVersionStr == null) {
            return "N/A";
        }
        Runtime.Version agentVersion;
        try {
            agentVersion = Runtime.Version.parse(agentVersionStr);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Failed to parse agent version: " + agentVersionStr, e);
            return "N/A";
        }
        final JVMVersionComparator jvmVersionComparator =
                new JVMVersionComparator(CONTROLLER_VERSION, agentVersion, comparisonMode);

        if (!isIgnored() && jvmVersionComparator.isNotCompatible()) {
            if (disconnect) {
                LOGGER.warning(
                        Messages.JVMVersionMonitor_MarkedOffline(c.getName(), CONTROLLER_VERSION, agentVersionStr));
                ((JvmVersionDescriptor) getDescriptor())
                        .markOffline(c, OfflineCause.create(Messages._JVMVersionMonitor_OfflineCause()));
            } else {
                LOGGER.finer("Version incompatibility detected, but keeping the agent '"
                        + c.getName()
                        + "' online per the node monitor configuration");
            }
        }
        return agentVersionStr;
    }

    public JVMVersionComparator.ComparisonMode getComparisonMode() {
        return comparisonMode;
    }

    @Extension
    public static class JvmVersionDescriptor extends AbstractAsyncNodeMonitorDescriptor<String> {

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.JVMVersionMonitor_DisplayName();
        }

        @Override
        protected Callable<String, IOException> createCallable(Computer c) {
            return new JavaVersion();
        }

        @Override // Just augmenting visibility
        public boolean markOffline(Computer c, OfflineCause oc) {
            return super.markOffline(c, oc);
        }

        public ListBoxModel doFillComparisonModeItems() {
            ListBoxModel items = new ListBoxModel();
            for (JVMVersionComparator.ComparisonMode goal : JVMVersionComparator.ComparisonMode.values()) {
                items.add(goal.getDescription(), goal.name());
            }
            return items;
        }
    }

    private static class JavaVersion extends MasterToSlaveCallable<String, IOException> {
        @Override
        public String call() {
            return Runtime.version().toString();
        }
    }
}
