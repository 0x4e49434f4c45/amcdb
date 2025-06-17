package network.parthenon.amcdb.discord.JDAMocks;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.requests.RestAction;

public class MockChannel implements MessageChannelUnion {

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

    @Override
    public PrivateChannel asPrivateChannel() {
        return null;
    }

    @Override
    public GroupChannel asGroupChannel() { return null; }

    @Override
    public TextChannel asTextChannel() {
        return null;
    }

    @Override
    public NewsChannel asNewsChannel() {
        return null;
    }

    @Override
    public ThreadChannel asThreadChannel() {
        return null;
    }

    @Override
    public VoiceChannel asVoiceChannel() {
        return null;
    }

    @Override
    public StageChannel asStageChannel() { return null; }

    @Override
    public IThreadContainer asThreadContainer() {
        return null;
    }

    @Override
    public GuildMessageChannel asGuildMessageChannel() {
        return null;
    }

    @Override
    public AudioChannel asAudioChannel() { return null; }

    @Override
    public long getLatestMessageIdLong() {
        return 0;
    }

    @Override
    public boolean canTalk() {
        return false;
    }

    @Override
    public boolean isDetached() { return false; }
}
