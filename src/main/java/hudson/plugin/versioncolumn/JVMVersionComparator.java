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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.util.VersionNumber;
import io.jenkins.lib.versionnumber.JavaSpecificationVersion;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Hex;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;


/**
 * Responsible for master and agent jvm versions comparisons, and notions of "compatibility".
 * <p>For instance, the default behaviour is to consider 1.8.0 compatible with 1.8.3-whatever and so on.
 * Only considering <em>major.minor</em> part, that is</p>
 */
class JVMVersionComparator {

    private static final Logger LOGGER = Logger.getLogger(JVMVersionComparator.class.getName());

    private static final Pattern MAJOR_MINOR_PATTERN = Pattern.compile("(\\d+\\.\\d+).*");
    private final MasterBytecodeMajorVersionNumberGetter masterBytecodeMajorVersionNumberGetter;
    private boolean compatible;

    JVMVersionComparator(String masterVersion, String agentVersion, ComparisonMode comparisonMode) {
        this(masterVersion, agentVersion, comparisonMode, new MasterBytecodeMajorVersionNumberGetter());
    }

    @VisibleForTesting
    JVMVersionComparator(String masterVersion, String agentVersion, ComparisonMode comparisonMode, MasterBytecodeMajorVersionNumberGetter versionNumberGetter) {
        masterBytecodeMajorVersionNumberGetter = versionNumberGetter;
        if (ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE == comparisonMode) {
            compatible = isAgentRuntimeCompatibleWithJenkinsBytecodeLevel(computeMajorMinor(agentVersion));
        } else if (ComparisonMode.EXACT_MATCH == comparisonMode) {
            compatible = masterVersion.equals(agentVersion);
        } else if (ComparisonMode.MAJOR_MINOR_MATCH == comparisonMode) {
            compatible = computeMajorMinor(masterVersion).equals(computeMajorMinor(agentVersion));
        }
    }

    @NonNull
    @VisibleForTesting
    static String computeMajorMinor(String version) {
        final Matcher matcher = MAJOR_MINOR_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(version + " is not a supported JVM version pattern");
        }
        return matcher.group(1);
    }

    /**
     * Reading the Jenkins.class bytecode first, then fallback to Jenkins.getVersion() is something is wrong.
     *
     * @return the bytecode major version of the Jenkins class (example: 51 for Java 7, 52 for Java 8...).
     * @see <a href="https://en.wikipedia.org/wiki/Java_class_file#General_layout">the Java bytecode general layout</a>.
     */
    private int getMasterBytecodeMajorVersionNumber() {
        return masterBytecodeMajorVersionNumberGetter.get();
    }

    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "JavaSpecificationVersion.fromReleaseVersion needs to be cleaned up to return null rather than throw NPE when an invalid version is passed in")
    private boolean isAgentRuntimeCompatibleWithJenkinsBytecodeLevel(String agentMajorMinorVersion) {
        int masterBytecodeLevel = getMasterBytecodeMajorVersionNumber();
        int agentVMMaxBytecodeLevel;
        try {
            int releaseVersion;
            if (agentMajorMinorVersion.startsWith("1.")) {
                releaseVersion = Integer.parseInt(agentMajorMinorVersion.split("\\.")[1]);
            } else {
                releaseVersion = Integer.parseInt(agentMajorMinorVersion.split("\\.")[0]);
            }
            JavaSpecificationVersion javaSpecificationVersion = JavaSpecificationVersion.fromReleaseVersion(releaseVersion);
            agentVMMaxBytecodeLevel = javaSpecificationVersion.toClassVersion();
        } catch (NullPointerException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, Messages.JVMVersionMonitor_UnrecognizedAgentJVM(agentMajorMinorVersion), e);
            /*
             * Even if the version might be compatible, we still mark the node as incompatible to prevent potential issues.
             */
            return false;
        }
        return masterBytecodeLevel <= agentVMMaxBytecodeLevel;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public boolean isNotCompatible() {
        return !isCompatible();
    }

    public enum ComparisonMode {
        RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE(Messages.JVMVersionMonitor_RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE()),
        MAJOR_MINOR_MATCH(Messages.JVMVersionMonitor_MAJOR_MINOR_MATCH()),
        EXACT_MATCH(Messages.JVMVersionMonitor_EXACT_MATCH());

        private String description;

        ComparisonMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

    }

    /**
     * Delegate exclusively dedicated to testability
     */
    @VisibleForTesting
    static class MasterBytecodeMajorVersionNumberGetter {

        public int get() {

            final URL location = Jenkins.class.getProtectionDomain().getCodeSource().getLocation();
            try (JarFile jarFile = new JarFile(location.getFile())) {
                final ZipEntry jenkinsClassEntry = jarFile.getEntry("jenkins/model/Jenkins.class");

                final InputStream inputStream = jarFile.getInputStream(jenkinsClassEntry);
                byte[] magicAndClassFileVersion = new byte[8];

                int read = inputStream.read(magicAndClassFileVersion);
                final String hexaBytes = Hex.encodeHexString(magicAndClassFileVersion);
                LOGGER.log(Level.FINE, "Jenkins.class file 8 first bytes: {0}", hexaBytes);
                if (read != 8 || !hexaBytes.startsWith("cafebabe")) {
                    throw new IllegalStateException("Jenkins.class content is abnormal: '" + hexaBytes + "'");
                }
                int javaMajor = (magicAndClassFileVersion[6] << 8) & 0xff00 |
                        magicAndClassFileVersion[7] & 0xff;
                LOGGER.log(Level.FINEST, "Bytecode major version {0}", javaMajor);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Issue while reading Jenkins.class bytecode level", e);
            }

            LOGGER.log(Level.FINE, "Falling back to using Jenkins.getVersion to infer bytecode level");
            VersionNumber jenkinsVersion = Jenkins.getVersion();
            if (jenkinsVersion == null) {
                throw new IllegalStateException("Jenkins.getVersion() returned a null value, stopping.");
            }
            // So Jenkins started with Java 1.4 (or less?) reading the *old* changelog (like around ~1.100)
            // but well not sure I'll bother
            if (jenkinsVersion.isOlderThan(new VersionNumber("1.520"))) {
                return JavaSpecificationVersion.JAVA_5.toClassVersion();
            } else if (jenkinsVersion.isOlderThan(new VersionNumber("1.612"))) {
                return JavaSpecificationVersion.JAVA_6.toClassVersion();
            } else if (jenkinsVersion.isOlderThan(new VersionNumber("2.54"))) {
                return JavaSpecificationVersion.JAVA_7.toClassVersion();
            } else if (jenkinsVersion.isNewerThan(new VersionNumber("2.54"))) {
                return JavaSpecificationVersion.JAVA_8.toClassVersion();
            }

            throw new IllegalStateException("Jenkins Bytecode Level could not be inferred");
        }
    }
}
