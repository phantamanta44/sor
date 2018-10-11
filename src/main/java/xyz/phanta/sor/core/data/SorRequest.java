package xyz.phanta.sor.core.data;

import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorResponse;

import javax.annotation.Nullable;

public class SorRequest<REQ, RES> implements ISorRequest<REQ, RES> {

    private final SorRequestType<REQ, RES> type;
    private final REQ body;

    public SorRequest(SorRequestType<REQ, RES> type, REQ body) {
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

    public static class Reponse<RES> implements ISorResponse<RES> {

        private final SorRequestType<?, RES> type;
        @Nullable
        private final RES body;

        public Reponse(SorRequestType<?, RES> type, @Nullable RES body) {
            this.type = type;
            this.body = body;
        }

        @Override
        public SorRequestType<?, RES> getType() {
            return type;
        }

        @Nullable
        @Override
        public RES getBody() {
            return body;
        }

    }

}
