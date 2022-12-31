package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.utils.MarkdownUtil;
import network.parthenon.amcdb.messaging.message.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.MessageUtils;
import network.parthenon.amcdb.messaging.message.TextComponent;

import java.util.*;

/**
 * This class provides a parser for Discord-flavored markdown into TextComponents.
 *
 * The parser does not perfectly replicate all the behavior of the AST-based parser in
 * the actual Discord client. For example, it does not properly handle evil cases like
 * "*****" (five asterisks), which Discord displays as a single bold asterisk and this
 * parser interprets as a single un-styled asterisk. It is, however, sufficient for all
 * but the most unusual of real life messages.
 */
class MarkdownParser {

    /**
     * Map associating markdown tokens with the styles they represent.
     */
    private static final Map<MarkdownToken.Type, InternalMessageComponent.Style> TOKEN_STYLES =
            Collections.unmodifiableMap(new EnumMap(MarkdownToken.Type.class) {{
                put(MarkdownToken.Type.SINGLE_ASTERISK, InternalMessageComponent.Style.ITALIC);
                put(MarkdownToken.Type.DOUBLE_ASTERISK, InternalMessageComponent.Style.BOLD);
                put(MarkdownToken.Type.SINGLE_UNDERSCORE, InternalMessageComponent.Style.ITALIC);
                put(MarkdownToken.Type.DOUBLE_UNDERSCORE, InternalMessageComponent.Style.UNDERLINE);
                put(MarkdownToken.Type.DOUBLE_TILDE, InternalMessageComponent.Style.STRIKETHROUGH);
            }});

    /**
     * Parses the given markdown into a list of appropriately styled TextComponents.
     *
     * This method does not perfectly replicate the AST-based parsing done in the
     * actual Discord client. It is sufficient for sane cases.
     *
     * @param markdown The markdown to parse.
     * @return List of TextComponents containing styled text.
     */
    public static List<TextComponent> toTextComponents(String markdown) {
        List<TextComponent> components = new ArrayList<>();

        EnumSet<InternalMessageComponent.Style> lastStyles = EnumSet.noneOf(InternalMessageComponent.Style.class);
        EnumSet<InternalMessageComponent.Style> activeStyles = EnumSet.noneOf(InternalMessageComponent.Style.class);

        StringBuilder currentContent = new StringBuilder();
        for(MarkdownToken token : getTokens(markdown)) {
            // Treat unmatched tokens as text
            if(token.type == MarkdownToken.Type.TEXT || !token.isMatched()) {
                // if our style has changed and we have content waiting to be put into a component,
                // add the component now.
                if(!lastStyles.equals(activeStyles)) {
                    if(!currentContent.isEmpty()) {
                        components.add(new TextComponent(currentContent.toString(), null, MessageUtils.copyStyleSet(lastStyles)));
                        currentContent.setLength(0);
                    }
                    lastStyles = MessageUtils.copyStyleSet(activeStyles);
                }
                currentContent.append(token.content);
            }
            else if(TOKEN_STYLES.containsKey(token.type)) {
                MessageUtils.toggleStyle(TOKEN_STYLES.get(token.type), activeStyles);
            }
        }

        // add last component if there is any text left
        if(!currentContent.isEmpty()) {
            components.add(new TextComponent(currentContent.toString(), null, MessageUtils.copyStyleSet(lastStyles)));
        }

        return components;
    }

    /**
     * Splits the given markdown into a list of MarkdownTokens.
     *
     * This does not get all possible tokens for Discord markdown -- only the ones
     * we need for styling.
     *
     * @param markdown Markdown to parse.
     * @return List of MarkdownTokens.
     */
    private static List<MarkdownToken> getTokens(String markdown) {
        List<MarkdownToken> tokens = new ArrayList<>();
        int currentTextTokenStart = 0;

        // This is used to quickly check whether we need to do a full search
        // for a matching token.
        EnumSet<MarkdownToken.Type> currentUnmatchedTokenTypes = EnumSet.noneOf(MarkdownToken.Type.class);

        int i;
        for(i = 0; i < markdown.length(); i++) {

            if(markdown.charAt(i) == '\\') {
                // escape sequence; skip
                i += 1;
                continue;
            }

            String twoCharSubstring = null;
            MarkdownToken.Type twoCharType = MarkdownToken.Type.TEXT;
            // try to match a two character token
            if(i < markdown.length() - 1) {
                twoCharSubstring = markdown.substring(i, i+2);
                twoCharType = MarkdownToken.Type.fromString(twoCharSubstring);
            }

            // also try to match a single character token
            String singleCharSubstring = markdown.substring(i, i+1);
            MarkdownToken.Type singleCharType = MarkdownToken.Type.fromString(singleCharSubstring);

            // now make a choice which token to use
            MarkdownToken token = null;
            // first of all, if both are text, nothing to do
            if(singleCharType == MarkdownToken.Type.TEXT && twoCharType == MarkdownToken.Type.TEXT) {
                continue;
            }
            // secondly, if only one is a style token, use that one
            if(singleCharType == MarkdownToken.Type.TEXT) {
                token = new MarkdownToken(twoCharType, twoCharSubstring);
            }
            else if(twoCharType == MarkdownToken.Type.TEXT) {
                token = new MarkdownToken(singleCharType, singleCharSubstring);
            }
            // if both are style tokens, prioritize the one that has the closer match candidate, if any
            // this makes sure that we choose tokens correctly in a situation like "***bold italic***"
            // if this too ends in a tie, choose the two character token
            else {
                int twoCharMatchIndex = currentUnmatchedTokenTypes.contains(twoCharType) ?
                        matchToken(twoCharType, tokens) : -1;
                int singleCharMatchIndex = currentUnmatchedTokenTypes.contains(singleCharType) ?
                        matchToken(singleCharType, tokens) : -1;

                token = singleCharMatchIndex > twoCharMatchIndex ?
                        new MarkdownToken(singleCharType, singleCharSubstring) :
                        new MarkdownToken(twoCharType, twoCharSubstring);
            }

            if(token.type == MarkdownToken.Type.SINGLE_ASTERISK
                    && !currentUnmatchedTokenTypes.contains(MarkdownToken.Type.SINGLE_ASTERISK)
                    && i < markdown.length() - 1
                    && markdown.charAt(i+1) == ' ') {
                // here we replicate an apparent bug in Discord
                // which causes italics not to be applied if the would-be opening
                // asterisk is followed by a space.
                // this is not the case for the single underscore, only the asterisk.
                continue;
            }

            if(currentTextTokenStart < i) {
                // we have content to add to a text token
                tokens.add(new MarkdownToken(MarkdownToken.Type.TEXT, markdown.substring(currentTextTokenStart, i)));
            }

            if(currentUnmatchedTokenTypes.contains(token.type)) {
                token.matchIndex = matchToken(token.type, tokens);
                // if matchToken() doesn't find a match, we now know that the
                // unmatched candidate token (if any) is unmatchable.
                // remove it from the unmatched tokens in either case.
                currentUnmatchedTokenTypes.remove(token.type);
                // if we found a match, mark the matched token with our index
                if(token.matchIndex != -1) {
                    tokens.get(token.matchIndex).matchIndex = tokens.size();
                }
            }
            else {
                currentUnmatchedTokenTypes.add(token.type);
            }
            tokens.add(token);

            // place currentTextTokenStart immediately after the token we took
            currentTextTokenStart = i + token.content.length();
            // if we took a two character token, we need to advance an extra character
            i += token.content.length() - 1;
        }

        // i is now just after the end of markdown (i.e., i == markdown.length())
        // check if we have characters to add to a final text token
        if(currentTextTokenStart < i) {
            tokens.add(new MarkdownToken(MarkdownToken.Type.TEXT, markdown.substring(currentTextTokenStart, i)));
        }

        return tokens;
    }

    /**
     * Helper function for {@link #getTokens(String)}.
     *
     * @param tokens Token list
     * @return Index of the matching token found, or -1 if none.
     */
    private static int matchToken(MarkdownToken.Type type, List<MarkdownToken> tokens) {
        int lastIndex = tokens.size() - 1;
        if (type == MarkdownToken.Type.TEXT) {
            // text tokens aren't matched
            return -1;
        }

        for (int i = tokens.size() - 1; i >= 0; i--) {
            MarkdownToken currentToken = tokens.get(i);
            if (currentToken.isMatched()) {
                // skip backwards to the first token in the pair
                // our matching token can't be between a matched pair
                i = currentToken.matchIndex;
                continue;
            }
            if (currentToken.type == type) {
                return i;
            }
        }

        return -1;
    }

    private static class MarkdownToken {
        public enum Type {
            TEXT,
            SINGLE_ASTERISK,
            DOUBLE_ASTERISK,
            SINGLE_UNDERSCORE,
            DOUBLE_UNDERSCORE,
            DOUBLE_TILDE;

            public static Type fromString(String content) {
                // quick check to get out of here as fast as possible
                if(content.length() > 2) {
                    return TEXT;
                }

                // manual map as we only have a few token types
                // if we had more, we'd use a HashMap instead
                if(content.equals("*")) {
                    return SINGLE_ASTERISK;
                }
                else if(content.equals("**")) {
                    return DOUBLE_ASTERISK;
                }
                else if(content.equals("_")) {
                    return SINGLE_UNDERSCORE;
                }
                else if(content.equals("__")) {
                    return DOUBLE_UNDERSCORE;
                }
                else if(content.equals("~~")) {
                    return DOUBLE_TILDE;
                }

                // if none of these matched, it's text
                return TEXT;
            }
        }

        public final Type type;

        public final String content;

        public int matchIndex = -1;

        public MarkdownToken(Type type, String content) {
            this.type = type;
            this.content = content;
        }

        public boolean isMatched() {
            return matchIndex > -1;
        }
    }
}
