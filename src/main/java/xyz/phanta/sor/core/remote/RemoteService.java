package xyz.phanta.sor.core.remote;

import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorRequestHandler;
import xyz.phanta.sor.api.request.ISorResponseCallback;
import xyz.phanta.sor.api.target.ISorRequestBus;
import xyz.phanta.sor.core.data.SorRequest;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

class RemoteService<REQ, RES> extends RemoteSubscriptionBus implements ISorRequestBus<REQ, RES> {

    private final SorRequestTarget<REQ, RES> service;
    private final Collection<ISorRequestHandler<REQ, RES>> handlers = new CopyOnWriteArrayList<>();

    RemoteService(RemoteApiImpl api, SorRequestTarget<REQ, RES> target) {
        super(api);
        this.service = target;
    }

    @Override
    public void handle(ISorRequestHandler<REQ, RES> handler) {
        checkListening();
        handlers.add(handler);
    }

    @Override
    public void request(REQ request, ISorResponseCallback<RES> callback) {
        api.getConnection().publishService(service, request, callback);
    }

    void request(ISorRequest<REQ, RES> request, ISorResponseCallback<RES> callback) {
        for (ISorRequestHandler<REQ, RES> handler : handlers) {
            RES result = handler.handleRequest(request);
            if (result != null) {
                callback.accept(new SorRequest.Reponse<>(service.getType(), result));
                return;
            }
        }
        callback.accept(new SorRequest.Reponse<>(service.getType(), null));
    }

    @Override
    void enableListen(CoreConnection connection) {
        connection.listenService(service);
    }

    SorRequestType<REQ, RES> getType() {
        return service.getType();
    }

}
