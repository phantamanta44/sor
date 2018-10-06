package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorRequest;

class SorRequest<REQ, RES> implements ISorRequest<REQ, RES> {

    private final SorRequestType<REQ, RES> type;
    private final REQ body;

    SorRequest(SorRequestType<REQ, RES> type, REQ body) {
        this.type = type;
        this.body = body;
    }

    @Override
    public SorRequestType<REQ, RES> getType() {
        return type;
    }

    @Override
    public REQ getBody() {
        return body;
    }

}
