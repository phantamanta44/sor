package xyz.phanta.sor.api.message;

import xyz.phanta.sor.api.exchange.SorMessageType;

public interface ISorMessage<MSG> {

    SorMessageType<MSG> getType();

    MSG getBody();

}
