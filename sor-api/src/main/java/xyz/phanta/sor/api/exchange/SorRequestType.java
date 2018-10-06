package xyz.phanta.sor.api.exchange;

public class SorRequestType<REQ, RES> {

    private final Class<REQ> requestClass;
    private final Class<RES> responseClass;

    public SorRequestType(Class<REQ> requestClass, Class<RES> responseClass) {
        this.requestClass = requestClass;
        this.responseClass = responseClass;
    }

    public Class<REQ> getRequestClass() {
        return requestClass;
    }

    public Class<RES> getResponseClass() {
        return responseClass;
    }

}
