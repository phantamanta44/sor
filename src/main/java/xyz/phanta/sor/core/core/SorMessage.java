package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.message.ISorMessage;

class SorMessage<MSG> implements ISorMessage<MSG> {

    private final SorMessageType<MSG> type;
    private final MSG body;

    SorMessage(SorMessageType<MSG> type, MSG body) {
        this.type = type;
        this.body = body;
    }

    @Override
    public SorMessageType<MSG> getType() {
        return type;
    }

    @Override
    public MSG getBody() {
        return body;
    }

}
