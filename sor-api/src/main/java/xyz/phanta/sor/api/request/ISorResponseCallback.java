package xyz.phanta.sor.api.request;

@FunctionalInterface
public interface ISorResponseCallback<RES> {

    void accept(ISorResponse<RES> response);

}
