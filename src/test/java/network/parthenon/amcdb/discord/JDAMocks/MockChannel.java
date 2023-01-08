package network.parthenon.amcdb.discord.JDAMocks;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.requests.RestAction;

public class MockChannel implements Channel {

    private final long channelId;

    public MockChannel(String id) {
        this(Long.parseLong(id));
    }

    public MockChannel(long id) {
        this.channelId = id;
    }

    @Override
    public String getName() {
        return "Channel%d".formatted(channelId);
    }

    @Override
    public ChannelType getType() {
        return ChannelType.TEXT;
    }

    @Override
    public JDA getJDA() {
        return null;
    }

    @Override
    public RestAction<Void> delete() {
        return null;
    }

    @Override
    public long getIdLong() {
        return channelId;
    }
}
