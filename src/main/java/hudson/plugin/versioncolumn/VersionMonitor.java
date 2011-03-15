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

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Callable;
import hudson.remoting.Launcher;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class VersionMonitor extends NodeMonitor {

    @Extension
    public static final class DescriptorImpl extends AbstractNodeMonitorDescriptor<String> {

        protected String monitor(Computer c) throws IOException, InterruptedException {
            return c.getChannel().call(new SlaveVersion());
        }

        public String getDisplayName() {
            return "Version";
        }

        @Override
        public NodeMonitor newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new VersionMonitor();
        }
    }

    private static final class SlaveVersion implements Callable<String, IOException> {

        private static final long serialVersionUID = 1L;

        public String call() throws IOException {
            try {
                return Launcher.VERSION;
            } catch (Throwable ex) {
                // Older slave.jar won't have VERSION
                return "< 1.335";
            } 
        }
    }
    
}