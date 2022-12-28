package network.parthenon.amcdb.messaging;

@FunctionalInterface
public interface MessageHandler {

    public void handleMessage(InternalMessage message);
}
