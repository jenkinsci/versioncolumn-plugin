package hudson.plugin.versioncolumn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shortens versions for display in the column; the full version is meant for a tooltip. */
final class VersionShortener {

    /** The numeric part of a version followed by a {@code .v<hash>} suffix, as used by remoting. */
    private static final Pattern HASH_SUFFIX = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\.v.*$");

    private VersionShortener() {}

    /** The JVM version {@code 25.0.4.1+1-LTS} as {@code 25.0.4.1}. Unparsable versions are returned as is. */
    static String jvm(String version) {
        if (version == null) {
            return null;
        }
        try {
            StringBuilder numbers = new StringBuilder();
            for (int component : Runtime.Version.parse(version).version()) {
                if (numbers.length() > 0) {
                    numbers.append('.');
                }
                numbers.append(component);
            }
            return numbers.toString();
        } catch (IllegalArgumentException e) {
            return version;
        }
    }

    /**
     * The remoting version {@code 3355.3357.v931d3c992987} as {@code 3355.3357}. A version without
     * a {@code .v<hash>} suffix is returned as is.
     */
    static String remoting(String version) {
        if (version == null) {
            return null;
        }
        Matcher matcher = HASH_SUFFIX.matcher(version);
        return matcher.matches() ? matcher.group(1) : version;
    }
}
