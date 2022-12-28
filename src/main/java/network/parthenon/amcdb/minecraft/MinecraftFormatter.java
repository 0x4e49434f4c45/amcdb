package network.parthenon.amcdb.minecraft;

import net.minecraft.text.Text;
import network.parthenon.amcdb.messaging.InternalMessageComponent;

import java.awt.*;

public class MinecraftFormatter {

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
        return new InternalMessageComponent[] { new network.parthenon.amcdb.messaging.Text(minecraftText.getString()) };
    }

    public static Color toJavaColor(int minecraftColorValue) {
        return new Color(minecraftColorValue);
    }

    public static int toMinecraftColorValue(Color color) {
        return color.getRGB();
    }
}
