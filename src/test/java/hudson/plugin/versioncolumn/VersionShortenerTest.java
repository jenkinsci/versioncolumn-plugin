package hudson.plugin.versioncolumn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class VersionShortenerTest {

    @Test
    void jvmKeepsNumbersOnly() {
        assertEquals("25.0.4.1", VersionShortener.jvm("25.0.4.1+1-LTS"));
        assertEquals("21", VersionShortener.jvm("21+35"));
    }

    @Test
    void jvmKeepsUnparsableVersion() {
        assertEquals("unknown", VersionShortener.jvm("unknown"));
        assertNull(VersionShortener.jvm(null));
    }

    @Test
    void remotingDropsHashSuffix() {
        assertEquals("3355.3357", VersionShortener.remoting("3355.3357.v931d3c992987"));
        assertEquals("3352", VersionShortener.remoting("3352.v17a_fb_4b_2773f"));
    }

    @Test
    void remotingKeepsVersionWithoutHash() {
        assertEquals("3355.3357", VersionShortener.remoting("3355.3357"));
        assertEquals("< 1.335", VersionShortener.remoting("< 1.335"));
        assertNull(VersionShortener.remoting(null));
    }
}
