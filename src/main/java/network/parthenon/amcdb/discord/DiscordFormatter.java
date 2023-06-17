package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.Timestamp;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.messaging.component.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Formats Discord raw content to and from InternalMessageComponent.
 */
public class DiscordFormatter {

    /**
     * Regex to identify mentions.
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<=^|[^\\\\])<((?:@&?|#|:[a-zA-Z0-9_]+:|t:)?)(\\d+)(:[RDdFfTt])?>");

    /**
     * Regex to identify escape sequences.
     *
     * Discord seems to interpret anything that's not alphanumeric or a space
     * as escapable.
     */
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\([^a-zA-Z0-9 ])");

    private final DiscordService discordService;

    private final DiscordConfig config;

    public DiscordFormatter(DiscordService discordService, DiscordConfig config) {
        this.discordService = discordService;
        this.config = config;
    }

    /**
     * Formats the provided raw Discord message and attachments into InternalMessageComponents.
     *
     * @param discordRawContent The content to parse
     * @param attachments       The attachments to format
     * @return InternalMessageComponents comprising the formatted content
     */
    public List<? extends InternalMessageComponent> toComponents(
            String discordRawContent,
            Iterable<Message.Attachment> attachments) {
        Stream.Builder<InternalMessageComponent> components = Stream.builder();
        boolean firstComponent = true;

        for(Message.Attachment attachment : attachments) {
            if(!firstComponent) {
                components.add(new TextComponent(" "));
            }
            firstComponent = false;
            components.accept(toUrlComponent(attachment));
        }

        if(!firstComponent) {
            components.add(new TextComponent(" "));
            return Stream.concat(components.build(), toComponentStream(discordRawContent)).toList();
        }
        return toComponents(discordRawContent);
    }

    /**
     * Formats the provided raw Discord message into InternalMessageComponents.
     *
     * @param discordRawContent The content to parse
     * @return InternalMessageComponents comprising the formatted content
     */
    public List<? extends InternalMessageComponent> toComponents(String discordRawContent) {
        return toComponentStream(discordRawContent).toList();
    }

    /**
     * Formats the provided raw Discord message into InternalMessageComponents.
     *
     * @param discordRawContent The content to parse
     * @return InternalMessageComponents comprising the formatted content
     */
    public Stream<? extends InternalMessageComponent> toComponentStream(String discordRawContent) {
        // Retrieve all of the referenced user IDs.
        CompletableFuture<Member>[] memberFutures = MENTION_PATTERN.matcher(discordRawContent).results()
                // retrieve only the user mentions; roles are always cached
                .filter(DiscordFormatter::isUserMatch)
                .map(r -> {
                    AMCDB.LOGGER.debug("Retrieving JDA Member object for id=%s", r.group(2));
                    return discordService.retrieveChatMemberById(r.group(2));
                })
                .toArray(size -> (CompletableFuture<Member>[]) new CompletableFuture[size]);

        // while we're waiting, parse the markdown to components
        List<SplittableInternalMessageComponent> components = MarkdownParser.toComponents(discordRawContent);

        // wait for requests to complete
        try {
            CompletableFuture.allOf(memberFutures).join();
        }
        catch (CompletionException e) {
            if(e.getCause() instanceof ErrorResponseException
                    && ((ErrorResponseException) e.getCause()).getErrorCode() == 10013) {
                AMCDB.LOGGER.warn("A mentioned member was not found in the Discord API.");
            }
            else {
                AMCDB.LOGGER.warn("Failed to retrieve at least one mentioned Discord member.", e);
            }
        }

        Map<String, Member> membersById = Arrays.stream(memberFutures)
                // filter out any that failed
                .filter(f -> !f.isCompletedExceptionally())
                .collect(Collectors.toUnmodifiableMap(f -> f.getNow(null).getId(), f -> f.getNow(null)));

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
                    newComponents.add(component.split(nextComponentStartIndex, matcher.start()));
                }

                newComponents.add(getMentionComponent(result, membersById));

                nextComponentStartIndex = matcher.end();
            } while (matcher.find());

            if(nextComponentStartIndex < component.getText().length()) {
                newComponents.add(component.split(nextComponentStartIndex));
            }

            return newComponents.stream();
        })
        // remove escapes at the last possible moment
        .map(c -> c instanceof TextComponent ? removeEscapes((TextComponent) c) : c);
    }

    /**
     * Gets an appropriately styled EntityReference component for a Member.
     * @param member The Member to style.
     * @param showAtSymbol Whether to prefix the display name with '@'.
     * @return EntityReference
     */
    public EntityReference getMemberReference(Member member, boolean showAtSymbol) {
        return new EntityReference(
                member.getId(),
                showAtSymbol ? "@" + getDisplayName(member) : getDisplayName(member),
                getUniqueName(member.getUser()),
                member.getColor(),
                EnumSet.of(InternalMessageComponent.Style.BOLD),
                getAvatarUrl(member));
    }

    /**
     * Gets an appropriately styled EntityReference component for a User.
     *
     * Use {@link #getMemberReference(Member, boolean)} when possible, as
     * nickname and color are unavailable for the User object.
     *
     * @param user The User to style.
     * @param showAtSymbol Whether to prefix the display name with '@'.
     * @return EntityReference
     */
    public EntityReference getUserReference(User user, boolean showAtSymbol) {
        return new EntityReference(
                user.getId(),
                showAtSymbol ? "@" + user.getEffectiveName() : user.getEffectiveName(),
                getUniqueName(user),
                null,
                EnumSet.of(InternalMessageComponent.Style.BOLD),
                user.getAvatarUrl());
    }

    /**
     * Gets an appropriately styled entity reference for the author of a Discord message.
     *
     * Uses the Member object for nickname and color if available; otherwise
     * falls back to the Author object.
     *
     * @param message The Message for which to get the author EntityReference.
     * @param showAtSymbol Whether to prefix the display name with '@'.
     * @return EntityReference
     */
    public EntityReference getAuthorReference(Message message, boolean showAtSymbol) {
        if(message.getMember() == null) {
            AMCDB.LOGGER.warn("Message member was null! Falling back to author; nickname will not be used.");
            return getUserReference(message.getAuthor(), showAtSymbol);
        }

        return getMemberReference(message.getMember(), showAtSymbol);
    }

    /**
     * Gets the User's unique ("real") name. If the account has been updated to a
     * new, globally unique username, returns the username. Otherwise, returns the
     * old name#0001 format.
     *
     * @param user The User for which to get the unique name.
     * @return Unique name
     */
    private String getUniqueName(User user) {
        if(user.getDiscriminator().equals("0000")) {
            return user.getName();
        }
        else {
            return user.getAsTag();
        }
    }

    /**
     * Transforms Discord mention syntax into an appropriate InternalMessageComponent.
     * @param result MatchResult containing a Discord mention.
     * @param memberMap
     * @return The InternalMessageComponent representing the Discord mention.
     */
    private InternalMessageComponent getMentionComponent(MatchResult result, Map<String, Member> memberMap) {
        if(isUserMatch(result)) {
            Member member = memberMap.get(result.group(2));
            if(member == null) {
                return new EntityReference(
                        result.group(1),
                        "@Unknown User",
                        "Could not find user %s".formatted(result.group()),
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD));
            }
            return getMemberReference(member, true);
        }
        else if(isRoleMatch(result)) {
            Role role = discordService.getRoleById(result.group(2));
            if(role == null) {
                return new TextComponent(result.group());
            }
            return new EntityReference(
                    role.getId(),
                    "@" + role.getName(),
                    null,
                    role.getColor(),
                    EnumSet.of(InternalMessageComponent.Style.BOLD));
        }
        else if(isChannelMatch(result)) {
            Channel channel = discordService.getChannelById(result.group(2));
            if(channel == null) {
                return new TextComponent(result.group());
            }
            return new EntityReference(
                    channel.getId(),
                    "#" + channel.getName(),
                    null,
                    null,
                    EnumSet.of(InternalMessageComponent.Style.BOLD));
        }
        else if(isTimestampMatch(result)) {
            Timestamp discordTimestamp = TimeFormat.parse(result.group());
            return new DateComponent(
                    discordTimestamp.getTimestamp(),
                    discordTimestamp.getFormat() == TimeFormat.RELATIVE ?
                            DateComponent.DateFormat.RELATIVE :
                            DateComponent.DateFormat.ABSOLUTE,
                    null,
                    EnumSet.of(InternalMessageComponent.Style.UNDERLINE));
        }

        // it's an emoji
        return new EntityReference(
                result.group(2),
                result.group(1),
                null,
                null,
                EnumSet.of(InternalMessageComponent.Style.BOLD));
    }

    /**
     * Replaces escape sequences in the component content with the
     * escaped character (i.e. removes the backslash).
     * @param component The component from which to remove escapes
     * @return Component with the escapes removed
     */
    private TextComponent removeEscapes(TextComponent component) {
        String content = component.getText();
        content = content.replaceAll(ESCAPE_PATTERN.pattern(), "$1");
        String altContent = component.getAltText();
        if(altContent != null && !"".equals(altContent)) {
            altContent = content.replaceAll(ESCAPE_PATTERN.pattern(), "$1");
        }

        // if the string hasn't changed, don't create a new component
        // String.replaceAll() returns the same string instance if it
        // doesn't find anything to replace
        return content == component.getText() && altContent == component.getAltText() ?
                component :
                new TextComponent(content, altContent, component.getColor(), component.getStyles());
    }

    /**
     * Gets the appropriate display name for a Member based on the
     * useServerNicknames setting.
     * @param member The Member for which to get a display name.
     * @return The display name.
     */
    public String getDisplayName(Member member) {
        return config.getDiscordUseServerNicknames() ? member.getEffectiveName() : member.getUser().getEffectiveName();
    }

    /**
     * Gets the appropriate avatar URL for a Member based on the
     * useServerNicknames setting.
     * @param member The Member for which to get an avatar URL.
     * @return The effective avatar URL if useServerNicknames is true;
     * otherwise the user's global avatar URL.
     */
    public String getAvatarUrl(Member member) {
        return config.getDiscordUseServerNicknames() ? member.getEffectiveAvatarUrl() : member.getUser().getAvatarUrl();
    }

    /**
     * Gets an appropriate UrlComponent to represent the specified message attachment.
     * @param attachment The attachment to format.
     * @return UrlComponent representing the attachment.
     */
    public UrlComponent toUrlComponent(Message.Attachment attachment) {
        return new UrlComponent(
                attachment.getUrl(),
                attachment.isImage() ? "<image>" : attachment.isVideo() ? "<video>" : "<file>"
        );
    }

    /**
     * Translates the provided components into raw Discord markdown, splitting the content
     * into strings of less than the specified number of characters.
     * @param components
     * @param charLimit
     * @return
     */
    public List<String> toDiscordRawContent(Stream<? extends InternalMessageComponent> components, int charLimit) {
        List<String> discordRawContent = new ArrayList<>();
        MarkdownBuilder markdownBuilder = new MarkdownBuilder(charLimit);

        Iterator<? extends InternalMessageComponent> componentIterator = components.iterator();
        while(componentIterator.hasNext()) {
            InternalMessageComponent component = componentIterator.next();
            if(component instanceof SplittableInternalMessageComponent) {
                SplittableInternalMessageComponent remainder;
                while((remainder = markdownBuilder.appendSplittableComponent((SplittableInternalMessageComponent) component)) != null) {
                    discordRawContent.add(markdownBuilder.toString());
                    markdownBuilder = new MarkdownBuilder(charLimit);
                }
            }
            else if(!markdownBuilder.appendComponent(component)) {
                discordRawContent.add(markdownBuilder.toString());
                markdownBuilder = new MarkdownBuilder(charLimit);
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

    private static boolean isTimestampMatch(MatchResult result) { return "t:".equals(result.group(1)); }
}
