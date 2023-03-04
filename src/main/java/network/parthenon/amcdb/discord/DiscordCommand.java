package network.parthenon.amcdb.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.data.services.PlayerMappingService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordCommand {

    /**
     * Used to validate and parse an input Discord tag.
     */
    private static final Pattern TAG_PATTERN = Pattern.compile("(?<username>[^ \t@#$:`]{2,32})#(?<discriminator>\\d{4})");

    private final DiscordService discordService;

    private final PlayerMappingService playerMappingService;

    public DiscordCommand(DiscordService discordService, PlayerMappingService playerMappingService) {
        this.discordService = discordService;
        this.playerMappingService = playerMappingService;
    }

    public void registerCommand(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("discord")
                .then(CommandManager.literal("link")
                        .then(CommandManager.argument("discordTag", StringArgumentType.greedyString())
                                .executes(this::link)
                        ))
                .then(CommandManager.literal("confirm")
                        .then(CommandManager.argument("code", StringArgumentType.greedyString())
                                .executes(this::confirm)
                        ))
                .then(CommandManager.literal("unlink")
                        .executes(this::unlink)
                )
        );
    }

    /**
     * Handles the /discord link command.
     * @param context
     * @return
     */
    private int link(CommandContext<ServerCommandSource> context) {
        String discordTag = context.getArgument("discordTag", String.class);
        Matcher tagMatcher = TAG_PATTERN.matcher(discordTag);
        if(!tagMatcher.find()) {
            context.getSource().sendError(Text.of("'%s' does not appear to be a valid Discord tag. A Discord tag looks like this: username#0000".formatted(discordTag)));
            return 1;
        }

        discordService.findChatMemberByUsernameAndDiscriminator(
                tagMatcher.group("username"),
                tagMatcher.group("discriminator"))
            .whenComplete((m, e) -> {
                if(m == null || e != null) {
                    context.getSource().sendError(Text.of("Could not find Discord user '%s'. Make sure your username is spelled correctly and you are a member of the Discord server.".formatted(discordTag)));
                    if(e != null) {
                        AMCDB.LOGGER.error("Failed to look up Discord user %s".formatted(discordTag), e);
                    }
                    return;
                }

                playerMappingService.createUnconfirmed(context.getSource().getPlayer().getUuid(), DiscordService.DISCORD_SOURCE_ID, m.getId())
                        .whenComplete((code, err) -> {
                            if(err != null) {
                                AMCDB.LOGGER.error(e.getMessage(), e);
                                context.getSource().sendError(Text.of("Something went wrong. Please ask your server admin to troubleshoot."));
                                return;
                            }
                            discordService.sendDirectMessage(m.getUser(), "Use the code `%s` to link your Discord and Minecraft accounts.".formatted(code))
                                    .whenComplete((v, error) -> {
                                        if(error != null) {
                                            AMCDB.LOGGER.error("Failed to send Discord user %s a DM.".formatted(discordTag), error);
                                            context.getSource().sendError(Text.of("Failed to send your confirmation code in a DM. Make sure the bot is allowed to DM you, then try again."));
                                            return;
                                        }
                                        context.getSource().sendFeedback(Text.of("Check your Discord DMs for your confirmation code, then use /discord confirm <code> to finish linking your account."), false);
                                    });
                        });
            });
        return 0;
    }

    /**
     * Handles the /discord confirm command.
     * @param context
     * @return
     */
    private int confirm(CommandContext<ServerCommandSource> context) {
        String code = context.getArgument("code", String.class);

        playerMappingService.confirm(context.getSource().getPlayer().getUuid(), DiscordService.DISCORD_SOURCE_ID, code)
                .whenComplete((success, e) -> {
                    if(e != null) {
                        AMCDB.LOGGER.error(e.getMessage(), e);
                        context.getSource().sendError(Text.of("Something went wrong. Please ask your server admin to troubleshoot."));
                        return;
                    }

                    if(success) {
                        context.getSource().sendFeedback(Text.of("Your Discord and Minecraft accounts are successfully linked."), false);
                    }
                    else {
                        context.getSource().sendError(Text.of("That code wasn't found. Please try again. Use /discord link again if you need another code."));
                    }
                });

        return 0;
    }

    /**
     * Handles the /discord unlink command.
     * @param context
     * @return
     */
    private int unlink(CommandContext<ServerCommandSource> context) {
        playerMappingService.remove(context.getSource().getPlayer().getUuid(), DiscordService.DISCORD_SOURCE_ID)
                .whenComplete((num, e) -> {
                    if(e != null) {
                        AMCDB.LOGGER.error(e.getMessage(), e);
                        context.getSource().sendError(Text.of("Something went wrong. Please ask your server admin to troubleshoot."));
                        return;
                    }

                    if(num == 0) {
                        context.getSource().sendFeedback(Text.of("You do not have a linked Discord account."), false);
                    }
                    else {
                        context.getSource().sendFeedback(Text.of("Your Discord account was successfully unlinked."), false);
                    }
                    return;
                });

        return 0;
    }
}
