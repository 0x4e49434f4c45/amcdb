package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.SplittableInternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.component.UrlComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.List;

class MarkdownParserTest {

    /**
     * Basic test that a string is converted to a component with the same content
     */
    @Test
    void unstyledTest() {
        markdownTest("abcd", List.of(new TextComponent("abcd")));
    }

    /**
     * Basic test of an italic span
     */
    @Test
    void simpleItalicTest() {
        markdownTest("*italic*", List.of(
                new TextComponent("italic", null, null, EnumSet.of(InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Basic test of a bold span
     */
    @Test
    void simpleBoldTest() {
        markdownTest("**bold**", List.of(
                new TextComponent("bold", null, null, EnumSet.of(InternalMessageComponent.Style.BOLD))
        ));
    }

    /**
     * Basic test of an italic span using underscores instead of asterisks
     */
    @Test
    void simpleItalicTestUnderscore() {
        markdownTest("_italic_", List.of(
                new TextComponent("italic", null, null, EnumSet.of(InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Basic test of an underline span
     */
    @Test
    void simpleUnderlineTest() {
        markdownTest("__underline__", List.of(
                new TextComponent("underline", null, null, EnumSet.of(InternalMessageComponent.Style.UNDERLINE))
        ));
    }

    /**
     * Basic test of a strikethrough span
     */
    @Test
    void simpleStrikethroughTest() {
        markdownTest("~~strikethrough~~", List.of(
                new TextComponent("strikethrough", null, null, EnumSet.of(InternalMessageComponent.Style.STRIKETHROUGH))
        ));
    }

    /**
     * Basic test that the obfuscated style is applied to spoiler text
     * and the text is also put in the alternate text field
     */
    @Test
    void simpleObfuscatedTest() {
        markdownTest("||spoiler||", List.of(
                new TextComponent("spoiler", "spoiler", null, EnumSet.of(InternalMessageComponent.Style.OBFUSCATED))
        ));
    }

    /**
     * Test that an unmatched token is treated as text
     */
    @Test
    void unmatchedStartingTokenTest() {
        markdownTest("*plain", List.of(
                new TextComponent("*plain", null, null, EnumSet.noneOf(InternalMessageComponent.Style.class))
        ));
    }

    /**
     * Test that an unmatched token at the end of the string is treated as test
     */
    @Test
    void unmatchedEndingTokenTest() {
        markdownTest("plain*", List.of(
                new TextComponent("plain*", null, null, EnumSet.noneOf(InternalMessageComponent.Style.class))
        ));
    }

    /**
     * Test that when two spans are nested, both styles are applied
     */
    @Test
    void nestedStyleTest() {
        markdownTest("**_bold italic_**", List.of(
                new TextComponent("bold italic", null, null, EnumSet.of(InternalMessageComponent.Style.BOLD, InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Test that when two spans are nested with extra text in the outer span,
     * both styles are applied to the inner span and only the outer style is applied
     * to the outer span
     */
    @Test
    void nestedStyleTest2() {
        markdownTest("~~strikethrough**bold strikethrough**strikethrough~~", List.of(
                new TextComponent("strikethrough", null, null, EnumSet.of(InternalMessageComponent.Style.STRIKETHROUGH)),
                new TextComponent("bold strikethrough", null, null, EnumSet.of(InternalMessageComponent.Style.BOLD, InternalMessageComponent.Style.STRIKETHROUGH)),
                new TextComponent("strikethrough", null, null, EnumSet.of(InternalMessageComponent.Style.STRIKETHROUGH))
        ));
    }

    /**
     * Test that when two nested spans exist that are indicated using the same symbol,
     * tokens are grouped correctly to give nested spans instead of overlapping spans
     */
    @Test
    void nestedStyleSameSymbolTest() {
        markdownTest("***bold italic***", List.of(
                new TextComponent("bold italic", null, null, EnumSet.of(InternalMessageComponent.Style.BOLD, InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Test that when two spans overlap, the leftmost span receives priority
     */
    @Test
    void overlappingStylePriorityTest() {
        markdownTest("*italic_italic*plain_", List.of(
                new TextComponent("italic_italic", null, null, EnumSet.of(InternalMessageComponent.Style.ITALIC)),
                new TextComponent("plain_", null, null, EnumSet.noneOf(InternalMessageComponent.Style.class))
        ));
    }

    /**
     * This test confirms we match a particular behavior of the real Discord client,
     * which is that an asterisk followed by a space cannot be the *opening* asterisk
     * of an italic span.
     */
    @Test
    void asteriskSpaceTest() {
        markdownTest("* plain*", List.of(new TextComponent("* plain*")));
    }

    /**
     * This test confirms we match a particular behavior of the real Discord client,
     * which is that an underscore followed by an alphanumeric character cannot be
     * the *closing* underscore of an italic span.
     */
    @Test
    void underscoreInWordTest() {
        markdownTest("Username_With_3_Underscores", List.of(new TextComponent("Username_With_3_Underscores")));
    }

    /**
     * Tests that a URL is properly detected and represented as a UrlComponent.
     */
    @Test
    void urlTest() {
        markdownTest("https://fake.url/", List.of(
                new UrlComponent("https://fake.url/")
        ));
    }

    /**
     * Tests that a URL is properly extracted as a UrlComponent when surrounded by other text.
     */
    @Test
    void inTextUrlTest() {
        markdownTest("check out this link (https://fake.url/) it's cool!", List.of(
                new TextComponent("check out this link ("),
                new UrlComponent("https://fake.url/"),
                new TextComponent(") it's cool!")
        ));
    }

    /**
     * Tests that style symbols, when matched, take priority over URLs
     * (matching Discord client behavior).
     */
    @Test
    void overlappingStyleUrlTest() {
        markdownTest("*italic https://fake.url/abcd*plain", List.of(
                new TextComponent("italic ", null, null, EnumSet.of(InternalMessageComponent.Style.ITALIC)),
                new UrlComponent("https://fake.url/abcd", "https://fake.url/abcd", null, EnumSet.of(
                        InternalMessageComponent.Style.ITALIC,
                        InternalMessageComponent.Style.UNDERLINE)),
                new TextComponent("plain")
        ));
    }

    /**
     * Tests that certain characters are excluded from a URL match, but only when they appear
     * at the end of a prospective URL!
     */
    @Test
    void endingExcludedCharsUrlTest() {
        markdownTest("Check out this link (https://fake.url/asdf;ghjkl), it's cool!", List.of(
                new TextComponent("Check out this link ("),
                new UrlComponent("https://fake.url/asdf;ghjkl", "https://fake.url/asdf;ghjkl"),
                new TextComponent("), it's cool!")
        ));
    }

    /**
     * Helper method for markdown tests
     * @param markdown The markdown input
     * @param expectedComponents The TextComponents to expect as output
     */
    void markdownTest(String markdown, List<SplittableInternalMessageComponent> expectedComponents) {
        List<SplittableInternalMessageComponent> actualComponents = MarkdownParser.toComponents(markdown);

        assertIterableEquals(expectedComponents, actualComponents);
    }
}