package network.parthenon.amcdb.minecraft;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.message.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.TextComponent;

import java.awt.Color;
import java.util.List;

public class MinecraftFormatter {

    /**
     * Prepended to chat messages sent by AMCDB.
     */
    public static final String AMCDB_MESSAGE_PREFIX = "[AMCDB] ";

    public static Text toMinecraftText(InternalMessage message) {
        MutableText mt = Text.empty();

        mt.append(AMCDB_MESSAGE_PREFIX);
        mt.append("(" + message.getSourceId() + ") ");
        mt.append(Text.literal(message.getAuthor().getDisplayName() + ": "));
        mt.append(toMinecraftText(message.getComponents()));

        return mt;
    }

    public static Text toMinecraftText(List<? extends InternalMessageComponent> components) {
        MutableText text = Text.empty();
        for(InternalMessageComponent component : components) {
            text.append(toMinecraftText(component));
        }
        return text;
    }

    public static Text toMinecraftText(InternalMessageComponent component) {
        Style textStyle = Style.EMPTY
                .withBold(component.getStyles().contains(InternalMessageComponent.Style.BOLD))
                .withItalic(component.getStyles().contains(InternalMessageComponent.Style.ITALIC))
                .withUnderline(component.getStyles().contains(InternalMessageComponent.Style.UNDERLINE))
                .withStrikethrough(component.getStyles().contains(InternalMessageComponent.Style.STRIKETHROUGH))
                .withObfuscated(component.getStyles().contains(InternalMessageComponent.Style.OBFUSCATED));

        if(MinecraftService.SHOW_TEXT_COLORS) {
            textStyle = textStyle.withColor(component.getColor() == null ? null : TextColor.fromRgb(toMinecraftColorValue(component.getColor())));
        }

        return Text.literal(component.getText()).setStyle(textStyle);
    }

    public static List<InternalMessageComponent> toComponents(Text minecraftText) {
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
