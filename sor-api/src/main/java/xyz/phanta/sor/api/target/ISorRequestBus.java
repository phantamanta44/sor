package xyz.phanta.sor.api.target;

import xyz.phanta.sor.api.request.ISorRequestHandler;
import xyz.phanta.sor.api.request.ISorResponseCallback;

public interface ISorRequestBus<REQ, RES> {

    void handle(ISorRequestHandler<REQ, RES> handler);

    void request(REQ request, ISorResponseCallback<RES> callback);

}
