package xyz.phanta.sor.api.message;

@FunctionalInterface
public interface ISorMessageListener<MSG> {

    void consume(ISorMessage<MSG> msg);

}
