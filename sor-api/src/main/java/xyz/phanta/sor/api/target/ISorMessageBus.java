package xyz.phanta.sor.api.target;

import xyz.phanta.sor.api.message.ISorMessageListener;

public interface ISorMessageBus<MSG> {

    void listen(ISorMessageListener<MSG> listener);

    void write(MSG message);

}
