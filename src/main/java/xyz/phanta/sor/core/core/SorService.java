package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorRequestHandler;
import xyz.phanta.sor.api.request.ISorResponseCallback;
import xyz.phanta.sor.api.target.ISorRequestBus;
import xyz.phanta.sor.core.data.SorRequest;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

class SorService<REQ, RES> implements ISorRequestBus<REQ, RES> {

    private final SorRequestType<REQ, RES> type;
    private final Collection<ISorRequestHandler<REQ, RES>> handlers = new CopyOnWriteArrayList<>();

    SorService(SorRequestType<REQ, RES> type) {
        this.type = type;
    }

    @Override
    public void handle(ISorRequestHandler<REQ, RES> handler) {
        handlers.add(handler);
    }

    void unhandle(ISorRequestHandler<REQ, RES> handler) {
        handlers.remove(handler);
    }

    @Override
    public void request(REQ request, ISorResponseCallback<RES> callback) {
        request(new SorRequest<>(type, request), callback);
    }

    void request(ISorRequest<REQ, RES> request, ISorResponseCallback<RES> callback) {
        for (ISorRequestHandler<REQ, RES> handler : handlers) {
            RES result = handler.handleRequest(request);
            if (result != null) {
                callback.accept(new SorRequest.Reponse<>(type, result));
                return;
            }
        }
        callback.accept(new SorRequest.Reponse<>(type, null));
    }

    SorRequestType<REQ, RES> getType() {
        return type;
    }

}
