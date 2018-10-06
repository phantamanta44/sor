package xyz.phanta.sor.api.request;

import xyz.phanta.sor.api.exchange.SorRequestType;

public interface ISorRequest<REQ, RES> {

    SorRequestType<REQ, RES> getType();

    REQ getBody();

}
