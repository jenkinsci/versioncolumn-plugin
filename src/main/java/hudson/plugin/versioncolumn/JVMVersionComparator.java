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

import java.util.List;

/**
 * Responsible for controller and agent jvm versions comparisons, and notions of "compatibility".
 */
class JVMVersionComparator {

    private boolean compatible;

    JVMVersionComparator(
            Runtime.Version controllerVersion, Runtime.Version agentVersion, ComparisonMode comparisonMode) {
        if (ComparisonMode.RUNTIME_GREATER_OR_EQUAL_MASTER_BYTECODE == comparisonMode) {
            compatible = agentVersion.feature() >= controllerVersion.feature();
        } else if (ComparisonMode.EXACT_MATCH == comparisonMode) {
            compatible = controllerVersion.version().equals(agentVersion.version());
        } else if (ComparisonMode.MAJOR_MINOR_MATCH == comparisonMode) {
            compatible = compareVersionList(agentVersion.version(), controllerVersion.version()) >= 0;
        }
    }

    /**
     * Compare the {@link Runtime.Version#version()} of two {@link Runtime.Version}s.
     *
     * @param o1 The {@link Runtime.Version#version()} of the first {@link Runtime.Version}.
     * @param o2 The {@link Runtime.Version#version()} of the seconds {@link Runtime.Version}.
     * @return A negative integer, zero, or a positive integer if the first {@link
     *     Runtime.Version#version()} is less than, equal to, or greater than the second {@link
     *     Runtime.Version#version()}.
     */
    private static int compareVersionList(List<Integer> o1, List<Integer> o2) {
        int size1 = o1.size();
        int size2 = o2.size();
        for (int i = 0; i < Math.min(size1, size2); i++) {
            int value1 = o1.get(i);
            int value2 = o2.get(i);
            if (value1 != value2) {
                return value1 - value2;
            }
        }
        return size1 - size2;
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
}
