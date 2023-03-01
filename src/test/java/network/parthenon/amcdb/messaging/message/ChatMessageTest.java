package network.parthenon.amcdb.messaging.message;

import network.parthenon.amcdb.messaging.component.EntityReference;
import network.parthenon.amcdb.messaging.component.InternalMessageComponent;
import network.parthenon.amcdb.messaging.component.TextComponent;
import network.parthenon.amcdb.minecraft.MinecraftService;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Text;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    public void testPlaceholders() {
        ChatMessage message = new ChatMessage(
                "JUNIT_TEST_SOURCE_ID",
                new EntityReference("authorId"),
                "messageText"
        );

        List<InternalMessageComponent> components = message.formatToComponents("%origin% %username% %message%");

        assertIterableEquals(List.of(
                new TextComponent("JUNIT_TEST_SOURCE_ID"),
                new TextComponent(" "),
                new EntityReference("authorId"),
                new TextComponent(" "),
                new TextComponent("messageText")
            ),
            components);
    }

}