package network.parthenon.amcdb.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import network.parthenon.amcdb.config.DiscordConfig;
import network.parthenon.amcdb.discord.JDAMocks.MockChannel;
import network.parthenon.amcdb.discord.JDAMocks.MockMember;
import network.parthenon.amcdb.discord.JDAMocks.MockRole;
import network.parthenon.amcdb.messaging.component.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DiscordFormatterTest {

    private DiscordConfig mockDiscordConfig;

    private DiscordService mockDiscordService;

    private DiscordFormatter formatter;

    @BeforeEach
    public void setupMockDiscordService() {
        mockDiscordService = Mockito.mock(DiscordService.class);
        mockDiscordConfig = Mockito.mock(DiscordConfig.class);

        Mockito.when(mockDiscordService.getChannelById(Mockito.any(String.class)))
                .thenAnswer(invocation -> new MockChannel(invocation.getArgument(0)));
        Mockito.when(mockDiscordService.getRoleById(Mockito.any(String.class)))
                .thenAnswer(invocation -> new MockRole(invocation.getArgument(0)));
        Mockito.when(mockDiscordService.getChatMemberFromCache(Mockito.any(String.class)))
                .thenAnswer(invocation -> new MockMember(invocation.getArgument(0)));
        Mockito.when(mockDiscordService.retrieveChatMemberById(Mockito.any(String.class)))
                .thenAnswer(invocation -> {
                    CompletableFuture<Member> future = new CompletableFuture<>();
                    future.complete(new MockMember(invocation.getArgument(0)));
                    return future;
                });

        formatter = new DiscordFormatter(mockDiscordService, mockDiscordConfig);
    }

    /**
     * Tests that backslashes from escape sequences do not appear in the formatted
     * output.
     */
    @Test
    public void escapeSequencesRemoved() {
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("\\<minecraftUsername\\>");

        assertIterableEquals(List.of(new TextComponent("<minecraftUsername>")), components);
    }

    /**
     * Tests that a user mention in the Discord message is extracted to a properly styled
     * EntityReference.
     */
    @Test
    public void userMention() {
        Mockito.when(mockDiscordConfig.getDiscordUseServerNicknames()).thenReturn(false);
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<@1234>");

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        "@Name1234",
                        "Name1234",
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that a user mention in the Discord message uses nickname when this is enabled.
     */
    @Test
    public void userMentionWithNickname() {
        Mockito.when(mockDiscordConfig.getDiscordUseServerNicknames()).thenReturn(true);
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<@1234>");

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        "@Nickname1234",
                        "Name1234",
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that a user mention is properly formatted when the account still uses
     * a discriminator (the #0001 part).
     */
    @Test
    public void userMentionWithDiscriminator() {
        // Set up the mock Discord service to return a discriminator that is not 0000
        Mockito.when(mockDiscordService.getChatMemberFromCache(Mockito.any(String.class)))
                .thenAnswer(invocation -> new MockMember(Long.parseLong(invocation.getArgument(0), 10), "0001"));
        Mockito.when(mockDiscordService.retrieveChatMemberById(Mockito.any(String.class)))
                .thenAnswer(invocation -> {
                    CompletableFuture<Member> future = new CompletableFuture<>();
                    future.complete(new MockMember(Long.parseLong(invocation.getArgument(0), 10), "0001"));
                    return future;
                });

        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<@1234>");

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        "@Name1234",
                        "Name1234#0001",
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that a role mention in the Discord message is extracted to a properly styled
     * EntityReference.
     */
    @Test
    public void roleMention() {
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<@&1234>");

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        "@Role1234",
                        null,
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that a channel mention in the Discord message is extracted to a properly styled
     * EntityReference.
     */
    @Test
    public void channelMention() {
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<#1234>");

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        "#Channel1234",
                        null,
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that a mentioned user is retrieved only once even if tagged multiple
     * times in a message.
     */
    @Test
    public void duplicateMention() {
        Mockito.when(mockDiscordConfig.getDiscordUseServerNicknames()).thenReturn(false);
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<@1234> <@1234> <@2345>");

        Mockito.verify(mockDiscordService, Mockito.times(2))
                .retrieveChatMemberById(Mockito.anyString());

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        "@Name1234",
                        "Name1234",
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD)),
                new TextComponent(" "),
                new EntityReference(
                        "1234",
                        "@Name1234",
                        "Name1234",
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD)),
                new TextComponent(" "),
                new EntityReference(
                        "2345",
                        "@Name2345",
                        "Name2345",
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that an emoji in the Discord message is extracted to a properly styled
     * EntityReference.
     */
    @Test
    public void emoji() {
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<:emoji:1234>");

        assertIterableEquals(List.of(
                new EntityReference(
                        "1234",
                        ":emoji:",
                        null,
                        null,
                        EnumSet.of(InternalMessageComponent.Style.BOLD))
        ), components);
    }

    /**
     * Tests that a relative timestamp in the Discord message is extracted to a
     * relative DateComponent
     */
    @Test
    public void relativeTimestamp() {
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<t:1673141400:R>");

        assertIterableEquals(List.of(
                new DateComponent(
                        1673141400L * 1000,
                        DateComponent.DateFormat.RELATIVE,
                        null,
                        EnumSet.of(InternalMessageComponent.Style.UNDERLINE)
                )
        ), components);
    }

    /**
     * Tests that an absolute timestamp in the Discord message is extracted to an
     * absolute DateComponent
     */
    @Test
    public void absoluteTimestamp() {
        List<? extends InternalMessageComponent> components =
                formatter.toComponents("<t:1673141400:D>");

        assertIterableEquals(List.of(
                new DateComponent(
                        1673141400L * 1000,
                        DateComponent.DateFormat.ABSOLUTE,
                        null,
                        EnumSet.of(InternalMessageComponent.Style.UNDERLINE)
                )
        ), components);
    }

    /**
     * Tests that an image attachment is properly prepended to a message as
     * a UrlComponent.
     */
    @Test
    public void imageAttachment() {
        Message.Attachment mockAttachment = Mockito.mock(Message.Attachment.class);
        Mockito.when(mockAttachment.isImage()).thenReturn(true);
        Mockito.when(mockAttachment.getUrl()).thenReturn("https://fake.attachment/");

        List<? extends InternalMessageComponent> components =
                formatter.toComponents("message", List.of(mockAttachment));

        assertIterableEquals(List.of(
                new UrlComponent(
                        "https://fake.attachment/",
                        "<image>"
                ),
                new TextComponent(" "),
                new TextComponent("message")
        ), components);
    }

    /**
     * Tests that a video attachment is properly prepended to a message as
     * a UrlComponent.
     */
    @Test
    public void videoAttachment() {
        Message.Attachment mockAttachment = Mockito.mock(Message.Attachment.class);
        Mockito.when(mockAttachment.isVideo()).thenReturn(true);
        Mockito.when(mockAttachment.getUrl()).thenReturn("https://fake.attachment/");

        List<? extends InternalMessageComponent> components =
                formatter.toComponents("message", List.of(mockAttachment));

        assertIterableEquals(List.of(
                new UrlComponent(
                        "https://fake.attachment/",
                        "<video>"
                ),
                new TextComponent(" "),
                new TextComponent("message")
        ), components);
    }

    /**
     * Tests that a file attachment is properly prepended to a message as
     * a UrlComponent.
     */
    @Test
    public void fileAttachment() {
        Message.Attachment mockAttachment = Mockito.mock(Message.Attachment.class);
        Mockito.when(mockAttachment.getUrl()).thenReturn("https://fake.attachment/");

        List<? extends InternalMessageComponent> components =
                formatter.toComponents("message", List.of(mockAttachment));

        assertIterableEquals(List.of(
                new UrlComponent(
                        "https://fake.attachment/",
                        "<file>"
                ),
                new TextComponent(" "),
                new TextComponent("message")
        ), components);
    }
}
