package network.parthenon.amcdb.minecraft;

import net.minecraft.network.chat.*;
import network.parthenon.amcdb.AMCDB;
import network.parthenon.amcdb.config.MinecraftConfig;
import network.parthenon.amcdb.messaging.component.UrlComponent;
import network.parthenon.amcdb.messaging.message.BroadcastMessage;
import network.parthenon.amcdb.messaging.message.ChatMessage;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;

import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MinecraftFormatter {

    private final MinecraftService minecraftService;

    private final MinecraftConfig config;

    public MinecraftFormatter(MinecraftService minecraftService, MinecraftConfig config) {
        this.minecraftService = minecraftService;
        this.config = config;
    }

    public Component toMinecraftComponent(ChatMessage message) {
        return toMinecraftComponent(message.formatToComponents(config.getMinecraftMessageFormat()));
    }

    public Component toMinecraftComponent(BroadcastMessage message) {
        return toMinecraftComponent(message.getComponents());
    }

    public Component toMinecraftComponent(List<? extends InternalMessageComponent> components) {
        MutableComponent text = Component.empty();
        for(InternalMessageComponent component : components) {
            text.append(toMinecraftComponent(component));
        }
        return text;
    }

    public Component toMinecraftComponent(InternalMessageComponent component) {
        Style textStyle = Style.EMPTY
                .withBold(component.getStyles().contains(InternalMessageComponent.Style.BOLD))
                .withItalic(component.getStyles().contains(InternalMessageComponent.Style.ITALIC))
                .withUnderlined(component.getStyles().contains(InternalMessageComponent.Style.UNDERLINE))
                .withStrikethrough(component.getStyles().contains(InternalMessageComponent.Style.STRIKETHROUGH))
                .withObfuscated(component.getStyles().contains(InternalMessageComponent.Style.OBFUSCATED));

        if(config.getMinecraftTextColorsEnabled()) {
            textStyle = textStyle.withColor(component.getColor() == null ? null : TextColor.fromRgb(toMinecraftColorValue(component.getColor())));
        }

        if(component.getAltText() != null) {
            //#if MC>=12105
            textStyle = textStyle.withHoverEvent(new HoverEvent.ShowText(Component.nullToEmpty(component.getAltText())));
            //#else
            //$$ textStyle = textStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.nullToEmpty(component.getAltText())));
            //#endif
        }

        if(component instanceof UrlComponent) {
            String urlString = ((UrlComponent) component).getUrl();
            try {
                // MC 1.21.5+ requires a valid Java URI. It's not a bad idea to validate it for all cases.
                URI parsedUri = new URI(urlString);
                //#if MC>=12105
                textStyle = textStyle.withClickEvent(new ClickEvent.OpenUrl(parsedUri));
                //#else
                //$$ textStyle = textStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlString));
                //#endif
            }
            catch (URISyntaxException e) {
                AMCDB.LOGGER.warn("Failed to parse URI '%s': %s".formatted(urlString, e.getMessage()));
            }
        }

        return Component.literal(component.getText()).setStyle(textStyle);
    }

    public List<InternalMessageComponent> toComponents(Component minecraftText) {
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
