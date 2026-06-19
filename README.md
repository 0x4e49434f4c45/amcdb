# AMCDB (Another Minecraft-Discord Bridge)

**AI-Free Software**

*AMCDB is proudly built by human hands, as a labor of love for the art of programming and the tradition of looking into the guts of a piece of software with one's own eyes.*

*No code within this mod was created by LLM or "AI" tools.*

## Overview

AMCDB is a Fabric/Quilt mod to connect your Minecraft server to a Discord
server. You can connect in-game chat to a Discord channel, set up a channel
for the server console, and even run server commands from anywhere!

**Here's what it looks like in Minecraft...**<br />
![Example of chat in Minecraft game](/doc/readme/assets/chat-example-minecraft.png)

**...and here's what it looks like in Discord!**<br />
![Example of chat in Discord channel](/doc/readme/assets/chat-example-discord.png)

In case you prefer to see in-game messages in Discord appear to come from
players (and see their player heads as avatars!), AMCDB supports Discord
webhooks as well:<br />
![Example of chat in Discord channel using webhook mode](/doc/readme/assets/chat-example-webhook.png)

## Key Features
- AMCDB is ***fast*** and ***lag-friendly***. It does almost all its work off 
the main server thread, so there's no impact to game performance.
- AMCDB is ***flexible***. You can configure everything just the way you want
it -- customize how messages look in game and in Discord, put the most useful
information for *you* in the channel topics, even give the Discord bot a clever
status message!
- AMCDB is ***stable*** and ***compatible***. It doesn't use mixins to modify
Minecraft code, so it rarely breaks between Minecraft versions and it works
well with just about any other Fabric mod.
- And lastly, AMCDB is ***fun***. It supports all sorts of cool Discord
features, like:
  - font styles...<br />
    ![Italic, bold, and underlined text in Minecraft](/doc/readme/assets/font-styles-example.png)
  - ...name colors...<br />
    ![Discord name color displayed in Minecraft](/doc/readme/assets/name-color-example.png)
  - ...and even spoiler text you can hover to reveal!<br />
    ![Discord spoiler text obfuscated in Minecraft](/doc/readme/assets/spoiler-text-example.png)

## Installation
Follow these steps to set up AMCDB on your server.

Note: you'll need to be an admin of the Discord server you want to connect.

### Install AMCDB on your Minecraft server
You need a Fabric or Quilt server. Once you have that set up, make sure
[Fabric API](https://modrinth.com/mod/fabric-api) is in your `mods` folder -
that's the one dependency AMCDB has.

Then, grab the latest AMCDB `.jar` file from
[GitHub Releases](https://github.com/0x4e49434f4c45/amcdb/releases/)
or [Modrinth](https://modrinth.com/mod/amcdb). It'll be something like
`amcdb-1.1.5.jar`. Put that in your `mods` folder.

Next, start up your server. It will crash -- that's just because you haven't
configured AMCDB yet. Now that you've started the server once, there should be
a file called `amcdb.properties` inside your `config` folder. Open that file in
your text editor and keep it around for the next step.

### Set up a Discord bot
*If you've used a different Minecraft-Discord mod before, you might already
have a bot. You can reuse it as long as it has the right permissions on your
server - just put the token in the `amcdb.properties` file.*

If you don't have a Discord bot:
1. Log into https://discord.com/developers/applications/.
2. Click **New Application** in the top right.
3. Give your bot a name, and click **Create**.<br />
   ![Discord Create Application dialog](/doc/readme/assets/discord-create-application.png)
4. On the left side of the screen, click **Bot**.<br />
   ![Discord Application menu](/doc/readme/assets/discord-application-menu.png)
5. Now you'll generate the token that AMCDB will use to connect. On the main
part of the page, find **Token** in the **Bot** section and click **Reset Token**.
Once you confirm and enter your 2FA code (if required), a random string of characters will
appear like the picture below -- that's your token! Click **Copy**.<br />
   ![Discord Bot token](/doc/readme/assets/discord-bot-token.png)
6. In the `amcdb.properties` file you opened earlier, find the line that starts
with `amcdb.discord.bot.token=` and paste your bot token on the end of that
line. It'll look like this:
    ```yaml
    amcdb.discord.bot.token=MTUxNjYyMDU5MTU2ODkxNjUyMQ.GtHHYH.cwfUiwbSCguEt8vA4KtdUdPlD-kexM_V98cSwE
    ```
7. Scroll down to the **Privileged Gateway Intents** section and turn on
**Message Content Intent**. This is what lets your bot read messages in your
Discord server so it can put them in your Minecraft game chat.<br />
   ![Discord Privileged Gateway Intents settings](/doc/readme/assets/discord-privileged-gateway-intents.png)
8. Back on the left side of the screen, move on to **OAuth2**.<br />
9. In the **OAuth2 URL Generator** section, under **Scopes**, checkmark the `bot` option.
Another box labeled **Bot Permissions** will appear. Here, select the following permissions:
   * Manage Channels
   * View Channels
   * Send Messages
   ![Discord Bot Permissions](/doc/readme/assets/discord-bot-permissions.png)
10. Then scroll down to the **Generated URL** section and copy the URL from the box. It
should look like this:
   ```
   https://discord.com/oauth2/authorize?client_id=1516620591568916521&permissions=3088&integration_type=0&scope=bot
   ```
11. Paste the URL you copied into a new browser tab, select a server, click
**Continue**, then **Authorize**. You should see the bot join your Discord
server!

### Configure the chat channel
AMCDB has a lot of settings you *can* configure, but besides the bot token,
there's only one you need to get started -- you need to tell AMCDB the ID of
the channel you want to use for the in game chat. You'll probably want to
create a new text channel for this, since all the messages in that channel
will appear in Minecraft and vice versa.

To get the channel ID, enable Developer Mode in your Discord app. Open
the settings menu, then **Advanced** on the left. Then, turn on **Developer
Mode**.<br />
![Discord Developer Mode setting](/doc/readme/assets/discord-developer-mode.png)

Once Developer mode is turned on, you can right click on the channel you want
to use and click **Copy ID**. Paste that ID into `amcdb.properties` right next
to `amcdb.discord.chat.channel=`. It'll look like this:
```yaml
amcdb.discord.chat.channel=1046313040837832782
```
If you want to set up a channel for the server console, copy its ID too and
paste it next to `amcdb.discord.console.channel=`. The console channel supports
running commands that you send via Discord; this feature is disabled by default
for safety, but you can enable it by changing the setting
`amcdb.discord.channels.console.enableExecution=` to `true`.

Then save the `amcdb.properties` file and start your Minecraft server again.
If you've done everything correctly, anything you type in the game chat
should appear in the Discord channel, and anything you type in the Discord
channel should appear in your game!

Feel free to explore the rest of the `amcdb.properties` file, as that's where
all of the configuration settings for AMCDB live. If you change anything,
you'll need to stop and start your Minecraft server for the new settings to
take effect.

### Configure webhook mode
If you want to display Minecraft player heads as Discord avatars, you'll need
to enable webhook mode. This is optional -- AMCDB will work fine if you don't
set up a webhook.

To set up a webhook, follow these steps:
1. Open your Discord server Settings page.
2. On the left, select Integrations (under the "Apps" category).
3. Click on "Webhooks", then "New Webhook".
4. Click on your new webhook, change its name to Minecraft (or whatever you 
   like), and set the channel to the channel you want to use for Minecraft
   chat.
5. Click "Copy Webhook URL".
6. Paste the URL into `amcdb.properties` next to `amcdb.discord.channels.chat.webhookUrl=`.
   Don't forget to uncomment that line by removing the `#` at the beginning.
7. As always, save the `amcdb.properties` file and restart your server. You
   should start to see in-game messages appearing in Discord with player names
   and avatars!

## Upgrade
Generally, you can upgrade AMCDB simply by replacing the `.jar` file with the
new version.

When you upgrade, it's a good idea to check whether new properties have been
added to `amcdb.properties`; you can find latest version of the default
configuration [here](/src/main/resources/amcdb.properties). Occasionally, new
required properties are added; this will cause your server to fail to start
after updating the `.jar` until you add the new properties to your
configuration.

You can also update your properties file by forcing AMCDB to generate a new one:
1. Stop your Minecraft server
2. Rename `amcdb.properties` to a temporary name, e.g. `amcdb.properties.old`
3. Update the `.jar` file and start your server. AMCDB will generate a new
   `amcdb.properties` template, and the server will stop just like with a new
   installation.
4. Copy your settings from `amcdb.properties.old` to the new `amcdb.properties`.
   - You can also simply copy any missing properties into `amcdb.properties.old`
     and then rename it back to `amcdb.properties`.
5. Start your Minecraft server.
