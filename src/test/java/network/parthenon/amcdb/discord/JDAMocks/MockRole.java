package network.parthenon.amcdb.discord.JDAMocks;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleIcon;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.managers.RoleManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.EnumSet;

public class MockRole implements Role {

    private final long roleId;

    public MockRole(String id) {
        this(Long.parseLong(id));
    }

    public MockRole(long id) {
        this.roleId = id;
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public int getPositionRaw() {
        return 0;
    }

    @Override
    public String getName() {
        return "Role%d".formatted(roleId);
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    public boolean isHoisted() {
        return false;
    }

    @Override
    public boolean isMentionable() {
        return false;
    }

    @Override
    public long getPermissionsRaw() {
        return 0;
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
    public boolean isPublicRole() {
        return false;
    }

    @Override
    public boolean canInteract(Role role) {
        return false;
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
    public RoleAction createCopy(Guild guild) {
        return null;
    }

    @Override
    public RoleManager getManager() {
        return null;
    }

    @Override
    public AuditableRestAction<Void> delete() {
        return null;
    }

    @Override
    public JDA getJDA() {
        return null;
    }

    @Override
    public RoleTags getTags() {
        return null;
    }

    @Override
    public RoleIcon getIcon() {
        return null;
    }

    @Override
    public int compareTo(@NotNull Role role) {
        return 0;
    }

    @Override
    public String getAsMention() {
        return "<@&%d>".formatted(roleId);
    }

    @Override
    public long getIdLong() {
        return roleId;
    }

    @Override
    public boolean isDetached() { return false; }
}
