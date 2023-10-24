/*
 * The MIT License
 *
 * Copyright (c) 2011, Seiji Sogabe
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Launcher;
import hudson.slaves.OfflineCause;
import java.io.IOException;
import java.util.logging.Logger;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class VersionMonitor extends NodeMonitor {

    private static final String masterVersion = Launcher.VERSION;

    @DataBoundConstructor
    public VersionMonitor () {
    }

    @SuppressWarnings("unused") // jelly
    public String toHtml(String version) {
        if (version == null) {
            return "N/A";
        }
        if (!version.equals(masterVersion)) {
            return Util.wrapToErrorSpan(version);
        }
        return version;
    }

    @SuppressFBWarnings(value = "MS_PKGPROTECT", justification = "for backward compatibility")
    public static /*almost final*/ AbstractNodeMonitorDescriptor<String> DESCRIPTOR;

    @Extension
    @Symbol("remotingVersion")
    public static class DescriptorImpl extends AbstractNodeMonitorDescriptor<String> {

        @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "for backward compatibility")
        public DescriptorImpl() {
            DESCRIPTOR = this;
        }

        protected String monitor(Computer c) throws IOException, InterruptedException {
            String version = c.getChannel().call(new SlaveVersion());
            if (version == null || !version.equals(masterVersion)) {
                if (!isIgnored()) {
                    markOffline(c, OfflineCause.create(Messages._VersionMonitor_OfflineCause()));
                    LOGGER.warning(Messages.VersionMonitor_MarkedOffline(c.getName()));
                }
            }
            return version;
        }

        @NonNull
        public String getDisplayName() {
            return Messages.VersionMonitor_DisplayName();
        }

        @Override
        public NodeMonitor newInstance(StaplerRequest req, @NonNull JSONObject formData) throws FormException {
            return new VersionMonitor();
        }
    };

    private static final class SlaveVersion extends MasterToSlaveCallable<String, IOException> {

        private static final long serialVersionUID = 1L;

        @Override
        public String call() throws IOException {
            try {
                return Launcher.VERSION;
            } catch (Throwable ex) {
                // Older slave.jar won't have VERSION
                return "< 1.335";
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(VersionMonitor.class.getName());
}
