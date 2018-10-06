package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.message.ISorMessageListener;
import xyz.phanta.sor.api.request.ISorRequestHandler;
import xyz.phanta.sor.api.target.ISorRequestBus;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class SorService<REQ, RES> implements ISorRequestBus<REQ, RES> {

    private final SorRequestType<REQ, RES> type;
    private final SorMessageType<RES> responseType;
    private final Collection<ISorRequestHandler<REQ, RES>> handlers = new CopyOnWriteArrayList<>();

    SorService(SorRequestType<REQ, RES> type) {
        this.type = type;
        this.responseType = new SorMessageType<>(type.getResponseClass());
    }

    @Override
    public void handle(ISorRequestHandler<REQ, RES> handler) {
        handlers.add(handler);
    }

    @Override
    public void request(REQ request, ISorMessageListener<RES> callback) {
        SorRequest<REQ, RES> wrapped = new SorRequest<>(type, request);
        for (ISorRequestHandler<REQ, RES> handler : handlers) {
            RES result = handler.handleRequest(wrapped);
            if (result != null) {
                callback.consume(new SorMessage<>(responseType, result));
                break;
            }
        }
    }

}
