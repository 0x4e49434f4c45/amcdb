package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.utils.concurrent.Task;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.message.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Formats Discord raw content to and from InternalMessageComponent.
 */
public class DiscordFormatter {

    private static final Pattern USER_REFERENCE_PATTERN = Pattern.compile("<@(\\d+)>");

    /**
     * Formats the provided raw Discord message into InternalMessageComponents.
     *
     * @param discordRawContent The content to parse
     * @return InternalMessageComponents comprising the formatted content
     */
    public static List<? extends InternalMessageComponent> toComponents(String discordRawContent) {
        // Pull all of the referenced user IDs into the cache ahead of time.
        CompletableFuture<Member>[] memberFutures = USER_REFERENCE_PATTERN.matcher(discordRawContent).results()
                .map(r -> r.group(1))
                .distinct()
                .map(id -> DiscordService.getInstance().retrieveChatMemberById(id))
                .toArray(size -> new CompletableFuture[size]);

        // while we're waiting, parse the markdown to components
        List<TextComponent> components = MarkdownParser.toTextComponents(discordRawContent);

        if(memberFutures.length == 0) {
            // no user references to intersperse
            return components;
        }

        // wait for requests to complete
        try {
            CompletableFuture.allOf(memberFutures).join();
        } catch (Exception e) {
            AMCDB.LOGGER.warn("Failed to retrieve mentioned Discord members; proceeding from cache.", e);
        }

        // intersperse the user references into the components
        return components.stream().flatMap(component -> {
            Matcher userMatcher = USER_REFERENCE_PATTERN.matcher(component.getText());

            if(!userMatcher.find()) {
                // no user references to replace in this component
                return Stream.of((InternalMessageComponent) component);
            }

            int nextComponentStartIndex = 0;
            List<InternalMessageComponent> newComponents = new ArrayList<>();

            do {
                if(nextComponentStartIndex < userMatcher.start()) {
                    newComponents.add(component.split(nextComponentStartIndex, userMatcher.start()));
                }
                Member member = DiscordService.getInstance().getChatMemberFromCache(userMatcher.group(1));
                newComponents.add(new UserReference(
                        member.getId(),
                        DiscordService.USE_NICKNAMES ? member.getEffectiveName() : member.getUser().getAsTag(),
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD)));
                nextComponentStartIndex = userMatcher.end() + 1;
            } while (userMatcher.find());

            if(nextComponentStartIndex < component.getText().length()) {
                newComponents.add(component.split(nextComponentStartIndex));
            }

            return newComponents.stream();
        })
        .toList();
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
