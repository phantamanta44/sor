package xyz.phanta.sor.api.request;

import xyz.phanta.sor.api.exchange.SorRequestType;

import javax.annotation.Nullable;

public interface ISorResponse<RES> {

    SorRequestType<?, RES> getType();

    @Nullable
    RES getBody();

}
