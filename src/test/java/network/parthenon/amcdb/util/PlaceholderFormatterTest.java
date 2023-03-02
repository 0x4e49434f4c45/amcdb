package network.parthenon.amcdb.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderFormatterTest {

    /**
     * Tests that when the format string contains no placeholders, the output is simply the format string.
     */
    @Test
    public void testNoPlaceholders() {
        assertEquals("noPlaceholder", PlaceholderFormatter.formatPlaceholders("noPlaceholder", Map.of()));
    }

    /**
     * Tests that a single placeholder is correctly replaced with its value.
     */
    @Test
    public void testSinglePlaceholder() {
        assertEquals("bar", PlaceholderFormatter.formatPlaceholders("%foo%", Map.of("%foo%", "bar")));
    }

    /**
     * Tests that when potential placeholders overlap, the leftmost is chosen.
     */
    @Test
    public void testOverlappingPlaceholders() {
        assertEquals("barfoobar", PlaceholderFormatter.formatPlaceholders("%foo%foo%foo%", Map.of("%foo%", "bar")));
    }

    /**
     * Tests that a bare word that corresponds to a placeholder is not replaced.
     */
    @Test
    public void testBarePlaceholder() {
        assertEquals("foo", PlaceholderFormatter.formatPlaceholders("foo", Map.of("%foo%", "bar")));
    }

    /**
     * Tests that a %-enclosed word that is not a placeholder is interpreted literally
     */
    @Test
    public void testInvalidPlaceholder() {
        assertEquals("%incorrectPlaceholder%", PlaceholderFormatter.formatPlaceholders("%incorrectPlaceholder%", Map.of("%correctPlaceholder%", "value")));
    }

    /**
     * Tests that escaped percents are unescaped, and correctly interpreted when part of placeholder names.
     */
    @Test
    public void testEscapedPercents() {
        assertEquals("not%placeholdervalue", PlaceholderFormatter.formatPlaceholders(
                "not\\%placeholder%placeholder\\%with\\%percents%",
                Map.of("%placeholder%with%percents%", "value")
        ));
    }
}