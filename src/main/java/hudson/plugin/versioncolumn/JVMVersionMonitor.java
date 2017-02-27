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

import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Callable;
import hudson.slaves.OfflineCause;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Logger;

public class JVMVersionMonitor extends NodeMonitor {

    public static final String JAVA_VERSION = "java.version";
    private static final String MASTER_VERSION = System.getProperty("java.version");
    private static final Logger LOGGER = Logger.getLogger(JVMVersionMonitor.class.getName());

    private boolean exactMatch;
    private boolean noDisconnect;

    @DataBoundConstructor
    public JVMVersionMonitor(boolean exactMatch, boolean noDisconnect) {
        this.exactMatch = exactMatch;
        this.noDisconnect = noDisconnect;
    }

    public JVMVersionMonitor() {
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public boolean isNoDisconnect() {
        return noDisconnect;
    }

    @Override
    public Object data(Computer c) {

        String agentVersion = (String) super.data(c);
        if (agentVersion == null) {
            return "N/A";
        }
        final JVMVersionComparator jvmVersionComparator =
                new JVMVersionComparator(MASTER_VERSION, agentVersion, exactMatch);

        if (!isIgnored() && jvmVersionComparator.isNotCompatible()) {
            if (noDisconnect) {
                LOGGER.finer(
                        "Version incompatibility detected, but keeping the agent '" + c.getName() + "' online per the node monitor configuration");
            } else {
                LOGGER.warning(Messages.JVMVersionMonitor_MarkedOffline(c.getName(), MASTER_VERSION, agentVersion));
                ((JvmVersionDescriptor) getDescriptor()).markOffline(c, OfflineCause.create(
                        Messages._JVMVersionMonitor_OfflineCause()));
            }
        }
        return agentVersion;
    }

    @Extension
    public static class JvmVersionDescriptor extends AbstractAsyncNodeMonitorDescriptor<String> {

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
    }

    private static class JavaVersion extends MasterToSlaveCallable<String, IOException> {
        @Override
        public String call() throws IOException {
            return System.getProperty(JAVA_VERSION);
        }
    }
}
