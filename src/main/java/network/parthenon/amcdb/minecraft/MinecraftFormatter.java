package network.parthenon.amcdb.minecraft;

import net.minecraft.text.*;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;

import java.awt.Color;
import java.util.List;

public class MinecraftFormatter {

    private final MinecraftService minecraftService;

    private final MinecraftConfig config;

    public MinecraftFormatter(MinecraftService minecraftService, MinecraftConfig config) {
        this.minecraftService = minecraftService;
        this.config = config;
    }

    public Text toMinecraftText(ChatMessage message) {
        return toMinecraftText(message.formatToComponents(config.getMinecraftMessageFormat()));
    }

    public Text toMinecraftText(BroadcastMessage message) {
        return toMinecraftText(message.getComponents());
    }

    public Text toMinecraftText(List<? extends InternalMessageComponent> components) {
        MutableText text = Text.empty();
        for(InternalMessageComponent component : components) {
            text.append(toMinecraftText(component));
        }
        return text;
    }

    public Text toMinecraftText(InternalMessageComponent component) {
        Style textStyle = Style.EMPTY
                .withBold(component.getStyles().contains(InternalMessageComponent.Style.BOLD))
                .withItalic(component.getStyles().contains(InternalMessageComponent.Style.ITALIC))
                .withUnderline(component.getStyles().contains(InternalMessageComponent.Style.UNDERLINE))
                .withStrikethrough(component.getStyles().contains(InternalMessageComponent.Style.STRIKETHROUGH))
                .withObfuscated(component.getStyles().contains(InternalMessageComponent.Style.OBFUSCATED));

        if(config.getMinecraftTextColorsEnabled()) {
            textStyle = textStyle.withColor(component.getColor() == null ? null : TextColor.fromRgb(toMinecraftColorValue(component.getColor())));
        }

        if(component.getAltText() != null) {
            textStyle = textStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(component.getAltText())));
        }

        return Text.literal(component.getText()).setStyle(textStyle);
    }

    public List<InternalMessageComponent> toComponents(Text minecraftText) {
        //TODO: implement rich conversion
        return List.of(new TextComponent(minecraftText.getString()));
    }

    public static Color toJavaColor(int minecraftColorValue) {
        return new Color(minecraftColorValue);
    }

    public static int toMinecraftColorValue(Color color) {
        // clear top 8 bits (normally used for opacity)
        // Minecraft doesn't like it when those bits are set on a text color
        return color.getRGB() & 0x00FFFFFF;
    }
}
