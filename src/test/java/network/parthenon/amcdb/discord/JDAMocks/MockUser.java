package network.parthenon.amcdb.discord.JDAMocks;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import java.util.EnumSet;
import java.util.List;

public class MockUser implements User {

    private final long userId;

    private final String discriminator;

    public MockUser(String id) {
        this(Long.parseLong(id));
    }

    public MockUser(long id) {
        this(id, "0000");
    }

    public MockUser(long id, String discriminator) {
        this.userId = id;
        this.discriminator = discriminator;
    }

    @Override
    public String getName() {
        return getGlobalName();
    }

    @Override
    public String getGlobalName() { return "Name%d".formatted(userId); }

    @Override
    public String getDiscriminator() {
        return discriminator;
    }

    @Override
    public String getAvatarId() {
        return null;
    }

    @Override
    public String getDefaultAvatarId() {
        return null;
    }

    @Override
    public CacheRestAction<Profile> retrieveProfile() {
        return null;
    }

    @Override
    public String getAsTag() {
        return "%s#%s".formatted(getName(), getDiscriminator());
    }

    @Override
    public boolean hasPrivateChannel() {
        return false;
    }

    @Override
    public CacheRestAction<PrivateChannel> openPrivateChannel() {
        return null;
    }

    @Override
    public List<Guild> getMutualGuilds() {
        return null;
    }

    @Override
    public boolean isBot() {
        return false;
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override
    public JDA getJDA() {
        return null;
    }

    @Override
    public EnumSet<UserFlag> getFlags() {
        return null;
    }

    @Override
    public int getFlagsRaw() {
        return 0;
    }

    @Override
    public String getAsMention() {
        return "<@%d>".formatted(userId);
    }

    @Override
    public long getIdLong() {
        return userId;
    }
}
