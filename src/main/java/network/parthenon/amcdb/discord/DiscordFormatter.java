package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.messaging.InternalMessageComponent;
import network.parthenon.amcdb.messaging.TextComponent;

/**
 * Formats Discord raw content to and from InternalMessageComponent.
 */
public class DiscordFormatter {

    public static InternalMessageComponent[] toComponents(String discordRawContent) {
        return new InternalMessageComponent[] { new TextComponent(discordRawContent) };
    }

    public static String toDiscordRawContent(InternalMessageComponent[] components) {
        //TODO: implement rich conversion
        StringBuilder sb = new StringBuilder();

        for(InternalMessageComponent component : components) {
            sb.append(escapeMarkdown(component.getText()));
        }

        return sb.toString();
    }

    public static String escapeMarkdown(String text) {
        //TODO: implement escaping
        return text;
    }
}
