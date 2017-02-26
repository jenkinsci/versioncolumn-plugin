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

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for master and agent jvm versions comparisons, and notions of "compatibility".
 * <p>For instance, the default behaviour is to consider 1.8.0 compatible with 1.8.3-whatever and so on.
 * Only considering <em>major.minor</em> part, that is</p>
 */
public class JVMVersionComparator {
    private static final Pattern MAJOR_MINOR_PATTERN = Pattern.compile("(\\d+\\.\\d+).*");
    private final String masterVersion;
    private final String agentVersion;

    public JVMVersionComparator(@Nonnull String masterVersion, String agentVersion, boolean exactMatch) {

        if (exactMatch) {
            this.masterVersion = masterVersion;
            this.agentVersion = agentVersion;
        } else {
            this.masterVersion = computeMajorMinor(masterVersion);
            this.agentVersion = computeMajorMinor(agentVersion);
        }
    }

    @VisibleForTesting
    static String computeMajorMinor(String version) {
        final Matcher matcher = MAJOR_MINOR_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(version + "is not a supported JVM version pattern");
        }
        return matcher.group(1);
    }

    public boolean isCompatible() {
        return masterVersion.equals(agentVersion);
    }

    public boolean isNotCompatible() {
        return !isCompatible();
    }
}
