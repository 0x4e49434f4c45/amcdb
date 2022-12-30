package network.parthenon.amcdb.discord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import network.parthenon.amcdb.messaging.message.*;

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
                new TextComponent("italic", null, EnumSet.of(InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Basic test of a bold span
     */
    @Test
    void simpleBoldTest() {
        markdownTest("**bold**", List.of(
                new TextComponent("bold", null, EnumSet.of(InternalMessageComponent.Style.BOLD))
        ));
    }

    /**
     * Basic test of an italic span using underscores instead of asterisks
     */
    @Test
    void simpleItalicTestUnderscore() {
        markdownTest("_italic_", List.of(
                new TextComponent("italic", null, EnumSet.of(InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Basic test of an underline span
     */
    @Test
    void simpleUnderlineTest() {
        markdownTest("__underline__", List.of(
                new TextComponent("underline", null, EnumSet.of(InternalMessageComponent.Style.UNDERLINE))
        ));
    }

    /**
     * Basic test of a strikethrough span
     */
    @Test
    void simpleStrikethroughTest() {
        markdownTest("~~strikethrough~~", List.of(
                new TextComponent("strikethrough", null, EnumSet.of(InternalMessageComponent.Style.STRIKETHROUGH))
        ));
    }

    /**
     * Test that an unmatched token is treated as text
     */
    @Test
    void unmatchedStartingTokenTest() {
        markdownTest("*plain", List.of(
                new TextComponent("*plain", null, EnumSet.noneOf(InternalMessageComponent.Style.class))
        ));
    }

    /**
     * Test that an unmatched token at the end of the string is treated as test
     */
    @Test
    void unmatchedEndingTokenTest() {
        markdownTest("plain*", List.of(
                new TextComponent("plain*", null, EnumSet.noneOf(InternalMessageComponent.Style.class))
        ));
    }

    /**
     * Test that when two spans are nested, both styles are applied
     */
    @Test
    void nestedStyleTest() {
        markdownTest("**_bold italic_**", List.of(
                new TextComponent("bold italic", null, EnumSet.of(InternalMessageComponent.Style.BOLD, InternalMessageComponent.Style.ITALIC))
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
                new TextComponent("strikethrough", null, EnumSet.of(InternalMessageComponent.Style.STRIKETHROUGH)),
                new TextComponent("bold strikethrough", null, EnumSet.of(InternalMessageComponent.Style.BOLD, InternalMessageComponent.Style.STRIKETHROUGH)),
                new TextComponent("strikethrough", null, EnumSet.of(InternalMessageComponent.Style.STRIKETHROUGH))
        ));
    }

    /**
     * Test that when two nested spans exist that are indicated using the same symbol,
     * tokens are grouped correctly to give nested spans instead of overlapping spans
     */
    @Test
    void nestedStyleSameSymbolTest() {
        markdownTest("***bold italic***", List.of(
                new TextComponent("bold italic", null, EnumSet.of(InternalMessageComponent.Style.BOLD, InternalMessageComponent.Style.ITALIC))
        ));
    }

    /**
     * Test that when two spans overlap, the leftmost span receives priority
     */
    @Test
    void overlappingStylePriorityTest() {
        markdownTest("*italic_italic*plain_", List.of(
                new TextComponent("italic_italic", null, EnumSet.of(InternalMessageComponent.Style.ITALIC)),
                new TextComponent("plain_", null, EnumSet.noneOf(InternalMessageComponent.Style.class))
        ));
    }

    /**
     * Test that escape sequences are not used to style text, and that the backslash is removed
     */
    @Test
    void escapeSequenceTest() {
        markdownTest("\\*plain*", List.of(
                new TextComponent("*plain*")
        ));
    }

    /**
     * Test that escape sequences are not used to style text, and that the backslash is removed,
     * when the escape sequence is the last thing in the string.
     */
    @Test
    void escapeSequenceEndTest() {
        markdownTest("*plain\\*", List.of(
                new TextComponent("*plain*")
        ));
    }

    /**
     * Test that only one backslash is removed when handling a backslash escape sequence
     */
    @Test
    void backslashEscapeSequenceTest() {
        markdownTest("plain\\\\", List.of(
                new TextComponent("plain\\")
        ));
    }

    /**
     * Test that a backslash is not removed unless it is followed by an actual
     * escapable character
     */
    @Test
    void aBackslashIsNotInItselfAnEscapeSequenceTest() {
        markdownTest("\\plain", List.of(
                new TextComponent("\\plain")
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
     * Helper method for markdown tests
     * @param markdown The markdown input
     * @param expectedComponents The TextComponents to expect as output
     */
    void markdownTest(String markdown, List<TextComponent> expectedComponents) {
        List<TextComponent> actualComponents = MarkdownParser.toTextComponents(markdown);

        assertIterableEquals(expectedComponents, actualComponents);
    }
}