package network.parthenon.amcdb.minecraft;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.messaging.component.UrlComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftFormatterTest {

    private MinecraftService mockMinecraftService;

    private MinecraftConfig mockMinecraftConfig;

    private MinecraftFormatter formatter;

    @BeforeEach
    public void setupMockMinecraftService() {
        mockMinecraftService = Mockito.mock(MinecraftService.class);
        mockMinecraftConfig = Mockito.mock(MinecraftConfig.class);
        formatter = new MinecraftFormatter(mockMinecraftService, mockMinecraftConfig);
    }

    /**
     * Tests that simple content is transformed to Minecraft text with the same content.
     */
    @Test
    public void content() {
        Text text = formatter.toMinecraftText(new TextComponent("text content"));

        assertEquals("text content", text.getString());
    }

    /**
     * Tests that color is applied to Minecraft text and opacity is stripped
     */
    @Test
    public void color() {
        Mockito.when(mockMinecraftConfig.getMinecraftTextColorsEnabled()).thenReturn(true);
        Text text = formatter.toMinecraftText(new TextComponent(
                "text content",
                null,
                new Color(0x115588BB),
                EnumSet.noneOf(InternalMessageComponent.Style.class)
        ));

        assertEquals(text.getStyle().getColor().getRgb(), 0x005588BB, "Minecraft text color is input color with opacity removed");
    }

    /**
     * Tests that color is not applied to Minecraft text when this feature is disabled
     */
    @Test
    public void colorDisabled() {
        Mockito.when(mockMinecraftConfig.getMinecraftTextColorsEnabled()).thenReturn(false);
        Text text = formatter.toMinecraftText(new TextComponent(
                "text content",
                null,
                new Color(0x115588BB),
                EnumSet.noneOf(InternalMessageComponent.Style.class)
        ));

        assertEquals(text.getStyle().getColor(), null, "Minecraft text color is null when color is disabled");
    }

    /**
     * Tests that all representable styles are added to Minecraft text.
     */
    @Test
    public void allStyles() {
        Text text = formatter.toMinecraftText(new TextComponent(
                "text content",
                null,
                null,
                EnumSet.allOf(InternalMessageComponent.Style.class)
        ));

        assertTrue(text.getStyle().isItalic(), "Minecraft text has italic style");
        assertTrue(text.getStyle().isBold(), "Minecraft text has bold style");
        assertTrue(text.getStyle().isUnderlined(), "Minecraft text has underlined style");
        assertTrue(text.getStyle().isStrikethrough(), "Minecraft text has strikethrough style");
        assertTrue(text.getStyle().isObfuscated(), "Minecraft text has obfuscated style");
    }

    /**
     * Tests that Minecraft text displays alt text on hover
     */
    @Test
    public void altTextHover() {
        Text text = formatter.toMinecraftText(new TextComponent(
                "text content",
                "alternate text content"
        ));

        assertEquals(
                Text.of("alternate text content"),
                text.getStyle().getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT));
    }

    /**
     * Tests that UrlComponent is transformed to Minecraft text with click action
     * to open the URL and that displays URL on hover
     */
    @Test
    public void url() {
        Text text = formatter.toMinecraftText(new UrlComponent(
                "https://fake.url/",
                "click here"
        ));

        assertEquals("click here", text.getString());
        assertEquals(
                Text.of("https://fake.url/"),
                text.getStyle().getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT));
        assertEquals(ClickEvent.Action.OPEN_URL, text.getStyle().getClickEvent().getAction());
        assertEquals("https://fake.url/", text.getStyle().getClickEvent().getValue());
    }

    /**
     * Tests that multiple components are properly concatenated into a single Text
     */
    @Test
    public void concatenated() {
        Text text = formatter.toMinecraftText(List.of(
                new TextComponent("click on this link: "),
                new UrlComponent("https://fake.url/"),
                new TextComponent(" (it'll be fun!)")
        ));

        assertEquals("click on this link: https://fake.url/ (it'll be fun!)", text.getString());
    }
}
