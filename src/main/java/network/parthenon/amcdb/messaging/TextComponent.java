package network.parthenon.amcdb.messaging;

import java.awt.*;

public class TextComponent implements InternalMessageComponent {

    private String text;

    private Color color;

    public TextComponent(String text) {
        this(text, null);
    }

    public TextComponent(String text, Color color) {
        if(text == null) {
            throw new IllegalArgumentException("text may not be null");
        }

        this.text = text;
        this.color = color;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getText() {
        return text;
    }
}
