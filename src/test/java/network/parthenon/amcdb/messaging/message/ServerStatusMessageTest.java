package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.DateComponent;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.minecraft.MinecraftService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerStatusMessageTest {

    /**
     * Tests that all placeholders are properly replaced.
     */
    @Test
    public void testPlaceholders() {
        ServerStatusMessage message = new ServerStatusMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                19.9D,
                3221225472L, // 3 GiB
                1073741824L, // 1 GiB
                3,
                10,
                List.of(new TextComponent("A Minecraft server")),
                1677651000000L
        );

        List<InternalMessageComponent> components = message.formatToComponents("%mspt% %tps% %freeMem% %usedMem% %totalMem% %playersOnline% %maxPlayers% %motd% %relativeTime% %absoluteTime%");

        assertIterableEquals(List.of(
                new TextComponent("19.9"),
                new TextComponent(" "),
                new TextComponent("20.0"),
                new TextComponent(" "),
                new TextComponent("1024.00MiB"),
                new TextComponent(" "),
                new TextComponent("2.00GiB"),
                new TextComponent(" "),
                new TextComponent("3.00GiB"),
                new TextComponent(" "),
                new TextComponent("3"),
                new TextComponent(" "),
                new TextComponent("10"),
                new TextComponent(" "),
                new TextComponent("A Minecraft server"),
                new TextComponent(" "),
                new DateComponent(1677651000000L, DateComponent.DateFormat.RELATIVE),
                new TextComponent(" "),
                new DateComponent(1677651000000L, DateComponent.DateFormat.ABSOLUTE)
            ),
            components);
    }

    /**
     * Tests that the TPS is correctly calculated from the MSPT.
     */
    @Test
    public void testTps() {
        ServerStatusMessage message = new ServerStatusMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                100.0D,
                3221225472L, // 3 GiB
                1073741824L, // 1 GiB
                3,
                10,
                List.of(new TextComponent("A Minecraft server")),
                1677651000000L
        );

        assertEquals(10.0D, message.getTps());
    }

    /**
     * Tests that the TPS is reported as 20.0 when the MSPT is small (<50).
     */
    @Test
    public void testMaximumTps() {
        ServerStatusMessage message = new ServerStatusMessage(
                MinecraftService.MINECRAFT_SOURCE_ID,
                1.0D,
                3221225472L, // 3 GiB
                1073741824L, // 1 GiB
                3,
                10,
                List.of(new TextComponent("A Minecraft server")),
                1677651000000L
        );

        assertEquals(20.0D, message.getTps());
    }
}