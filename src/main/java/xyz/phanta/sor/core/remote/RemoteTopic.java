package xyz.phanta.sor.core.remote;

import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.message.ISorMessageListener;
import xyz.phanta.sor.api.target.ISorMessageBus;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

class RemoteTopic<MSG> extends RemoteSubscriptionBus implements ISorMessageBus<MSG> {

    private final SorMessageTarget<MSG> topic;
    private final ExecutorService threadPool;
    private final Collection<ISorMessageListener<MSG>> listeners = new CopyOnWriteArrayList<>();

    RemoteTopic(RemoteApiImpl api, SorMessageTarget<MSG> target, ExecutorService threadPool) {
        super(api);
        this.topic = target;
        this.threadPool = threadPool;
    }

    @Override
    public void listen(ISorMessageListener<MSG> listener) {
        checkListening();
        listeners.add(listener);
    }

    @Override
    public void write(MSG message) {
        api.getConnection().publishTopic(topic, message);
    }

    void write(ISorMessage<MSG> message) {
        for (ISorMessageListener<MSG> listener : listeners) {
            threadPool.submit(() -> listener.consume(message));
        }
    }

    @Override
    void enableListen(CoreConnection connection) {
        connection.listenTopic(topic);
    }

    SorMessageType<MSG> getType() {
        return topic.getType();
    }

}
