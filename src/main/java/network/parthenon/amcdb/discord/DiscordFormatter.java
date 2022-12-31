package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.messaging.message.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Formats Discord raw content to and from InternalMessageComponent.
 */
public class DiscordFormatter {

    /**
     * Regex to identify mentions.
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<=^|[^\\\\])<((?:@&?|#|:[a-zA-Z0-9_]+:)?)(\\d+)>");

    /**
     * Regex to identify escape sequences.
     *
     * Discord seems to interpret anything that's not alphanumeric or a space
     * as escapable.
     */
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\([^a-zA-Z0-9 ])");

    /**
     * Formats the provided raw Discord message into InternalMessageComponents.
     *
     * @param discordRawContent The content to parse
     * @return InternalMessageComponents comprising the formatted content
     */
    public static List<? extends InternalMessageComponent> toComponents(String discordRawContent) {
        // Pull all of the referenced user IDs into the cache ahead of time.
        CompletableFuture<Member>[] memberFutures = MENTION_PATTERN.matcher(discordRawContent).results()
                // retrieve only the user mentions; roles are always cached
                .filter(DiscordFormatter::isUserMatch)
                .map(r -> DiscordService.getInstance().retrieveChatMemberById(r.group(2)))
                .toArray(size -> new CompletableFuture[size]);

        // while we're waiting, parse the markdown to components
        List<TextComponent> components = MarkdownParser.toTextComponents(discordRawContent);

        // wait for requests to complete
        try {
            CompletableFuture.allOf(memberFutures).join();
        }
        catch (CompletionException e) {
            if(e.getCause() instanceof ErrorResponseException
                    && ((ErrorResponseException) e.getCause()).getErrorCode() == 10013) {
                AMCDB.LOGGER.warn("A mentioned user was not found on the Discord API.");
            }
            else {
                AMCDB.LOGGER.warn("Failed to retrieve mentioned Discord members; proceeding from cache.", e);
            }
        }

        // intersperse the user references into the components
        // prepare for mixed-paradigm Stream chaos
        return components.stream().flatMap(component -> {
            Matcher matcher = MENTION_PATTERN.matcher(component.getText());

            if(!matcher.find()) {
                // no user references to replace in this component
                return Stream.of((InternalMessageComponent) component);
            }

            int nextComponentStartIndex = 0;
            List<InternalMessageComponent> newComponents = new ArrayList<>();

            do {
                MatchResult result = matcher.toMatchResult();

                if(nextComponentStartIndex < matcher.start()) {
                    // remove escapes at the last possible moment
                    newComponents.add(removeEscapes(component.split(nextComponentStartIndex, matcher.start())));
                }

                newComponents.add(getMentionComponent(result));

                nextComponentStartIndex = matcher.end() + 1;
            } while (matcher.find());

            if(nextComponentStartIndex < component.getText().length()) {
                // remove escapes at the last possible moment
                newComponents.add(removeEscapes(component.split(nextComponentStartIndex)));
            }

            return newComponents.stream();
        })
        .toList();
    }

    private static InternalMessageComponent getMentionComponent(MatchResult result) {
        if(isUserMatch(result)) {
            Member member = DiscordService.getInstance().getChatMemberFromCache(result.group(2));
            if(member == null) {
                return new TextComponent(result.group());
            }
            return new EntityReference(
                    member.getId(),
                    "@" + getDisplayName(member),
                    member.getColor(),
                    EnumSet.of(InternalMessageComponent.Style.BOLD));
        }
        else if(isRoleMatch(result)) {
            Role role = DiscordService.getInstance().getRoleById(result.group(2));
            if(role == null) {
                return new TextComponent(result.group());
            }
            return new EntityReference(
                    role.getId(),
                    "@" + role.getName(),
                    role.getColor(),
                    EnumSet.of(InternalMessageComponent.Style.BOLD));
        }
        else if(isChannelMatch(result)) {
            Channel channel = DiscordService.getInstance().getChannelById(result.group(2));
            if(channel == null) {
                return new TextComponent(result.group());
            }
            return new EntityReference(
                    channel.getId(),
                    "#" + channel.getName(),
                    null,
                    EnumSet.of(InternalMessageComponent.Style.BOLD));
        }

        // it's an emoji
        return new EntityReference(
                result.group(2),
                result.group(1),
                null,
                EnumSet.of(InternalMessageComponent.Style.BOLD));
    }

    /**
     * Replaces escape sequences in the component content with the
     * escaped character (i.e. removes the backslash).
     * @param component The component from which to remove escapes
     * @return Component with the escapes removed
     */
    private static TextComponent removeEscapes(TextComponent component) {
        String content = component.getText();

        content = content.replaceAll(ESCAPE_PATTERN.pattern(), "$1");

        // if the string hasn't changed, don't create a new component
        // String.replaceAll() returns the same string instance if it
        // doesn't find anything to replace
        return content == component.getText() ?
                component :
                new TextComponent(content, component.getColor(), component.getStyles());
    }

    public static String getDisplayName(Member member) {
        return DiscordService.USE_NICKNAMES ? member.getEffectiveName() : member.getUser().getAsTag();
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

    private static boolean isUserMatch(MatchResult result) {
        return "@".equals(result.group(1));
    }

    private static boolean isRoleMatch(MatchResult result) {
        return "@&".equals(result.group(1));
    }

    private static boolean isChannelMatch(MatchResult result) {
        return "#".equals(result.group(1));
    }

    public static String escapeMarkdown(String text) {
        //TODO: implement escaping
        return text;
    }
}
