package network.parthenon.amcdb.minecraft;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import network.parthenon.amcdb.messaging.message.InternalMessage;
import network.parthenon.amcdb.messaging.message.InternalMessageComponent;
import network.parthenon.amcdb.messaging.message.TextComponent;

import java.awt.*;

public class MinecraftFormatter {

    /**
     * Prepended to chat messages sent by AMCDB.
     */
    public static final String AMCDB_MESSAGE_PREFIX = "[AMCDB] ";

    public static Text toMinecraftText(InternalMessage message) {
        MutableText mt = MutableText.of(TextContent.EMPTY);

        mt.append(AMCDB_MESSAGE_PREFIX);
        mt.append("(" + message.getSourceId() + ") ");
        mt.append(Text.literal(message.getAuthor().getDisplayName() + ": "));
        mt.append(toMinecraftText(message.getComponents()));

        return mt;
    }

    public static Text toMinecraftText(InternalMessageComponent[] components) {
        //TODO: implement rich conversion
        StringBuilder sb = new StringBuilder();

        for(InternalMessageComponent component : components) {
            sb.append(component.getText());
        }

        return Text.of(sb.toString());
    }

    public static InternalMessageComponent[] toComponents(Text minecraftText) {
        //TODO: implement rich conversion
        return new InternalMessageComponent[] { new TextComponent(minecraftText.getString()) };
    }

    public static Color toJavaColor(int minecraftColorValue) {
        return new Color(minecraftColorValue);
    }

    public static int toMinecraftColorValue(Color color) {
        return color.getRGB();
    }
}
