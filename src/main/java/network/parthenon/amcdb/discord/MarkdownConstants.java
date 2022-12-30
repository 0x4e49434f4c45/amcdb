package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.messaging.message.InternalMessageComponent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

class MarkdownConstants {

    /**
     * List of characters that require escaping to display literally in Discord.
     */
    public static final List<Character> ESCAPE_CHARS = List.of('\\', '*', '_', '~', '`');

    /**
     * Regular expression that matches escape sequences in Discord markdown,
     * giving the escaped character as the first capture group.
     */
    public static final String ESCAPE_REGEX = "\\\\([\\\\*_~`])";
}
