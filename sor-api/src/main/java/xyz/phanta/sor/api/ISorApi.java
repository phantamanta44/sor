package xyz.phanta.sor.api;

import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.target.ISorMessageBus;
import xyz.phanta.sor.api.target.ISorRequestBus;

public interface ISorApi {

    <MSG> ISorMessageBus<MSG> getMessageBus(SorMessageTarget<MSG> target);

    <REQ, RES> ISorRequestBus<REQ, RES> getRequestBus(SorRequestTarget<REQ, RES> target);

}
