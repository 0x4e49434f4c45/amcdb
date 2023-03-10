# Multi-Server Configuration

AMCDB supports installation on multiple Minecraft servers. You can configure
multi-server installations in several different ways, depending on what makes
sense for your situation.

To start with, make sure AMCDB is installed in the mods folder on each of your
Minecraft servers. Then, read through the other sections of this page to understand
how the different AMCDB configuration options can be used in multi-server setups.

### Discord configuration
#### Bot account
You can use the same Discord bot account for all your servers. If you do want to use
separate bot accounts, be aware that AMCDB won't be able to identify messages sent
from other bots -- so you'll want to make sure that they don't post to the same
channel in your Discord server. If you use the same bot account, this isn't a problem.

#### Chat channel configuration
AMCDB isn't designed to sync in game chat across Minecraft servers, so if you want to
do that, you'll need to use a separate mod (but you can still use AMCDB to connect
everything to Discord).
- If you have a separate mod for cross-server chat:
  - It will probably work best to pick one Minecraft server that will post all the
    chat messages to Discord. The rest should have `amcdb.discord.chat.channel`
    disabled in the configuration.
- If you _do not_ have a separate mod for cross-server chat:
  - If you want a separate text channel for each Minecraft server, simply set the
    appropriate channel ID or webhook in each AMCDB configuration file.
  - If you want all the chat messages to go to the same Discord text channel, you
    can do that (but messages that users post to that Discord channel will be sent
    to all the Minecraft servers, which might be confusing). In this case, you
    might want to change `amcdb.discord.chatMessageFormat`,
    `amcdb.discord.broadcastMessageFormat`, and
    `amcdb.discord.webhookChatMessageFormat` (if you are using webhooks) to include
    the name of the server, so you can tell where the messages came from in Discord.
    For example, if you have a survival server and a creative testing server, you
    could set
    ```properties
    amcdb.discord.chatMessageFormat=[Survival] <%username%> %message%
    ```
    and
    ```properties
    amcdb.discord.chatMessageFormat=[Creative] <%username%> %message%
    ```
    in the respective configuration files.
  
    ***If multiple servers are sharing a chat channel, you MUST disable
    `amcdb.discord.channels.chat.topicFormat` on all but one server.***
    Failing to do this will cause the servers to update the channel topic
    on top of each other, and in the worst case scenario could result in
    too many topic updates and get your bot banned.

#### Console channel configuration
It is not recommended to have multiple servers share a single console channel.
If you want to use the console feature, create a separate text channel for each
server console.

#### Role configuration
If you want to have a Discord role automatically given to players when they're
online in a Minecraft server, you can use a separate role for each server or the
same role for multiple servers. AMCDB supports both scenarios equally well. If
you set the same role on multiple servers, the role will be given to a player
as long as they are on any one of the Minecraft servers set up to use that role.
For the latter configuration to work properly, you must set up a shared database
(more on that in the next section).

### Database
In its default configuration, AMCDB automatically sets up an on-disk H2 database
which it uses to store Minecraft player IDs and which Discord accounts they've
linked. In single-server mode, this works out of the box without any further
configuration. However, in a multi-server setup, this means that players have
to individually link their Discord account on every server, which isn't ideal.
Furthermore, since each server has a separate database, they can't sync Discord
role information between themselves, so the online role feature may not work as
well and players might not always be in the correct role.

To solve this problem, AMCDB supports using a shared database. This is highly
recommended for any multi-server setup. You can set up AMCDB to connect to several
popular databases, including PostgreSQL, MySQL, and MariaDB. SQLite, Oracle, and
DB2 are not supported.

_Important_: for a shared database to work properly, all servers must be running
the same version of AMCDB.

#### Network database (e.g. PostgreSQL, MySQL, MariaDB)
To set up a database other than H2, you'll need to provide the correct JDBC database
driver. This will be a `.jar` file. You can get it [here](https://jdbc.postgresql.org/download/)
for PostgreSQL or [here](https://dev.mysql.com/downloads/connector/j/?os=26) for MySQL
(grab the platform-independent version and extract the `.zip` to get the `.jar`).
AMCDB doesn't provide built-in drivers for anything other than H2, primarily because
building in additional drivers would increase the size of the mod significantly.

Once you've obtained the correct driver `.jar` for your database, place it in your
server folder alongside your Minecraft server `.jar`. Then, you'll need to tell
Java to load it when Minecraft starts. For that, you'll need to change your startup
command slightly. For example, if you have this in your startup script:
```shell
java -jar fabric-server.jar --nogui
```
You'll need to change it to this:
```shell
java -cp postgresql-42.5.4.jar:fabric-server.jar net.fabricmc.installer.ServerLauncher --nogui
```
Replace `postgresql-42.5.4.jar` with whatever driver `.jar` you have.

The last step is to edit `amcdb.database.url` in your configuration to the correct
URL. For example, for a PostgreSQL database, you might have
`jdbc:postgresql://db.example.com:5432/amcdb`. Also edit `amcdb.database.username`
and `amcdb.database.password` to the correct values.

When connecting to a brand new database for the first time, start up only one
Minecraft server and wait for it to fully start before starting your other servers.
After the first startup, you can start your servers simultaneously without any
issues.

#### Shared database: the quick and dirty way
H2 supports an "auto server" mode which can be easily enabled to create a shared
database. This method does have some drawbacks:
- All your servers must have access to the same database file *and* must be able
  to communicate with each other over a network port.
- If your servers are not all on the same physical or virtual machine, you will
  need to forward an additional port on each server (if a firewall is enabled) and
  arrange for some type of network storage for the database file.
- The H2 database server runs in the same process as your Minecraft server, which
  means it can use up memory and CPU resources. In a large multi-server environment,
  this may have negative performance impacts.
- If you decide later to move to a proper network database, migrating the data
  to the new database will be a challenge.

This method is ***not recommended*** if your servers are not all run on the same
(physical or virtual) machine. Even if that is the case today, using this method
will limit your options if you later need more resources and want to  run them on
separate machines. However, the auto-server method does offer a quicker setup,
and is particularly useful for testing scenarios.

To use the auto server mode, modify `amcdb.database.url` so that the path portion
(after `jdbc:h2:file:`) points to a location that all your servers can access.
Then, add `;AUTO_SERVER=TRUE` to the end of the URL. It will end up looking
something like this:
```properties
amcdb.database.url=jdbc:h2:file:/path/to/amcdb.h2;AUTO_SERVER=TRUE
```

Now, all your servers will automatically use the same H2 database. By default, a
random unused network port is used for the server. If you need to use a particular
port, add on `;AUTO_SERVER_NETWORK_PORT=<port>` to your URL.
