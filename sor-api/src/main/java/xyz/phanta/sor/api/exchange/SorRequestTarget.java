package xyz.phanta.sor.api.exchange;

public class SorRequestTarget<REQ, RES> {

    private final String name;
    private final SorRequestType<REQ, RES> type;

    public SorRequestTarget(String name, SorRequestType<REQ, RES> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public SorRequestType<REQ, RES> getType() {
        return type;
    }

}
