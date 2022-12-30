package network.parthenon.amcdb.discord;

import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.message.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.SplittableInternalMessageComponent;
import network.parthenon.amcdb.messaging.message.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Formats Discord raw content to and from InternalMessageComponent.
 */
public class DiscordFormatter {

    public static List<? extends InternalMessageComponent> toComponents(String discordRawContent) {
        return MarkdownParser.toTextComponents(discordRawContent);
    }

    public static List<String> toDiscordRawContent(InternalMessage message) {
        Stream<? extends InternalMessageComponent> components;

        if(message.getAuthor() != null) {
            components = Stream.concat(
                    Stream.of(new TextComponent("<" + DiscordFormatter.escapeMarkdown(message.getAuthor().getDisplayName()) + "> ")),
                    message.getComponents().stream()
            );
        }
        else {
            components = message.getComponents().stream();
        }

        return toDiscordRawContent(components);
    }

    /**
     * Translates the provided components into raw Discord markdown, splitting the content
     * into strings of less than {@link DiscordService#DISCORD_MESSAGE_CHAR_LIMIT} characters.
     * @param components
     * @return
     */
    public static List<String> toDiscordRawContent(Stream<? extends InternalMessageComponent> components) {
        List<String> discordRawContent = new ArrayList<>();
        MarkdownBuilder markdownBuilder = new MarkdownBuilder(DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);

        Iterator<? extends InternalMessageComponent> componentIterator = components.iterator();
        while(componentIterator.hasNext()) {
            InternalMessageComponent component = componentIterator.next();
            if(component instanceof SplittableInternalMessageComponent) {
                SplittableInternalMessageComponent remainder;
                while((remainder = markdownBuilder.appendSplittableComponent((SplittableInternalMessageComponent) component)) != null) {
                    discordRawContent.add(markdownBuilder.toString());
                    markdownBuilder = new MarkdownBuilder(DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);
                }
            }
            else if(!markdownBuilder.appendComponent(component)) {
                discordRawContent.add(markdownBuilder.toString());
                markdownBuilder = new MarkdownBuilder(DiscordService.DISCORD_MESSAGE_CHAR_LIMIT);
                if(!markdownBuilder.appendComponent(component)) {
                    AMCDB.LOGGER.warn("Non-splittable component was too large to fit in a Discord message! Skipping this component.");
                }
            }
        }

        if(markdownBuilder.length() > 0) {
            discordRawContent.add(markdownBuilder.toString());
        }

        return discordRawContent;
    }

    public static String escapeMarkdown(String text) {
        //TODO: implement escaping
        return text;
    }
}
