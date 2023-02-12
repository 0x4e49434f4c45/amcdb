package network.parthenon.amcdb.discord.JDAMocks;

import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.managers.AccountManager;

public class MockSelfUser extends MockUser implements SelfUser {

    public MockSelfUser() {
        super(99999999);
    }

    @Override
    public long getApplicationIdLong() {
        return 1L;
    }

    @Override
    public boolean isVerified() {
        return false;
    }

    @Override
    public boolean isMfaEnabled() {
        return false;
    }

    @Override
    public long getAllowedFileSize() {
        return 0;
    }

    @Override
    public AccountManager getManager() {
        return null;
    }
}
