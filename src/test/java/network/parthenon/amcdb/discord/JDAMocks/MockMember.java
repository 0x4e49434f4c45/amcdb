package network.parthenon.amcdb.discord.JDAMocks;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class MockMember implements Member {

    private final long memberId;

    private final MockUser user;

    public MockMember(String id) {
        this(Long.parseLong(id));
    }

    public MockMember(long id) {
        this(id, "0000");
    }

    public MockMember(long id, String discriminator) {
        this.memberId = id;
        this.user = new MockUser(id, discriminator);
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Guild getGuild() {
        return null;
    }

    @Override
    public EnumSet<Permission> getPermissions() {
        return null;
    }

    @Override
    public EnumSet<Permission> getPermissions(GuildChannel channel) {
        return null;
    }

    @Override
    public EnumSet<Permission> getPermissionsExplicit() {
        return null;
    }

    @Override
    public EnumSet<Permission> getPermissionsExplicit(GuildChannel channel) {
        return null;
    }

    @Override
    public boolean hasPermission(Permission... permissions) {
        return false;
    }

    @Override
    public boolean hasPermission(Collection<Permission> permissions) {
        return false;
    }

    @Override
    public boolean hasPermission(GuildChannel channel, Permission... permissions) {
        return false;
    }

    @Override
    public boolean hasPermission(GuildChannel channel, Collection<Permission> permissions) {
        return false;
    }

    @Override
    public boolean canSync(IPermissionContainer targetChannel, IPermissionContainer syncSource) {
        return false;
    }

    @Override
    public boolean canSync(IPermissionContainer channel) {
        return false;
    }

    @Override
    public JDA getJDA() {
        return null;
    }

    @Override
    public OffsetDateTime getTimeJoined() {
        return null;
    }

    @Override
    public boolean hasTimeJoined() {
        return false;
    }

    @Override
    public OffsetDateTime getTimeBoosted() {
        return null;
    }

    @Override
    public boolean isBoosting() {
        return false;
    }

    @Override
    public OffsetDateTime getTimeOutEnd() {
        return null;
    }

    @Override
    public GuildVoiceState getVoiceState() {
        return null;
    }

    @Override
    public List<Activity> getActivities() {
        return null;
    }

    @Override
    public OnlineStatus getOnlineStatus() {
        return null;
    }

    @Override
    public OnlineStatus getOnlineStatus(ClientType type) {
        return null;
    }

    @Override
    public EnumSet<ClientType> getActiveClients() {
        return null;
    }

    @Override
    public String getNickname() {
        return "Nickname%d".formatted(memberId);
    }

    @Override
    public String getEffectiveName() {
        return getNickname();
    }

    @Override
    public String getAvatarId() {
        return null;
    }

    @Override
    public List<Role> getRoles() {
        return null;
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public int getColorRaw() {
        return 0;
    }

    @Override
    public int getFlagsRaw() {
        return 0;
    }

    @Override
    public boolean canInteract(Member member) {
        return false;
    }

    @Override
    public boolean canInteract(Role role) {
        return false;
    }

    @Override
    public boolean canInteract(RichCustomEmoji emoji) {
        return false;
    }

    @Override
    public boolean isOwner() {
        return false;
    }

    @Override
    public boolean isPending() {
        return false;
    }

    @Override
    public DefaultGuildChannelUnion getDefaultChannel() {
        return null;
    }

    @Override
    public String getAsMention() {
        return "<@%d>".formatted(memberId);
    }

    @Override
    public long getIdLong() {
        return memberId;
    }

    @Override
    public String getDefaultAvatarId() {
        return null;
    }
}
