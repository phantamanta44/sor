package xyz.phanta.sor.api.target;

import xyz.phanta.sor.api.message.ISorMessageListener;
import xyz.phanta.sor.api.request.ISorRequestHandler;

public interface ISorRequestBus<REQ, RES> {

    void handle(ISorRequestHandler<REQ, RES> handler);

    void request(REQ request, ISorMessageListener<RES> callback);

}
