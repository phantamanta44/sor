package xyz.phanta.sor.api.exchange;

public class SorMessageTarget<MSG> {

    private final String name;
    private final SorMessageType<MSG> type;

    public SorMessageTarget(String name, SorMessageType<MSG> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public SorMessageType<MSG> getType() {
        return type;
    }

}
