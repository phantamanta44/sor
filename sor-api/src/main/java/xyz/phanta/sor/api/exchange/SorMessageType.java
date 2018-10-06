package xyz.phanta.sor.api.exchange;

public class SorMessageType<MSG> {

    private final Class<MSG> messageClass;

    public SorMessageType(Class<MSG> messageClass) {
        this.messageClass = messageClass;
    }

    public Class<MSG> getMessageClass() {
        return messageClass;
    }

}
