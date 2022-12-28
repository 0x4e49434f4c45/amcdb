package network.parthenon.amcdb.messaging;

import java.awt.*;

public class Text implements InternalMessageComponent {

    private String text;

    private Color color;

    public Text(String text) {
        this(text, null);
    }

    public Text(String text, Color color) {
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
