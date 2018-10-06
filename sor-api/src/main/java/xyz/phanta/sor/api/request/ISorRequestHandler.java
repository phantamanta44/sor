package xyz.phanta.sor.api.request;

import javax.annotation.Nullable;

@FunctionalInterface
public interface ISorRequestHandler<REQ, RES> {

    @Nullable
    RES handleRequest(ISorRequest<REQ, RES> request);

}
