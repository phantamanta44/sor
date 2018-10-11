package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.message.ISorMessageListener;
import xyz.phanta.sor.api.target.ISorMessageBus;
import xyz.phanta.sor.core.data.SorMessage;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

class SorTopic<MSG> implements ISorMessageBus<MSG> {

    private final SorMessageType<MSG> type;
    private final ExecutorService threadPool;
    private final Collection<ISorMessageListener<MSG>> listeners = new CopyOnWriteArrayList<>();

    SorTopic(SorMessageType<MSG> type, ExecutorService threadPool) {
        this.type = type;
        this.threadPool = threadPool;
    }

    @Override
    public void listen(ISorMessageListener<MSG> listener) {
        listeners.add(listener);
    }

    void unlisten(ISorMessageListener<MSG> listener) {
        listeners.remove(listener);
    }

    @Override
    public void write(MSG message) {
        write(new SorMessage<>(type, message));
    }

    void write(ISorMessage<MSG> message) {
        for (ISorMessageListener<MSG> listener : listeners) {
            threadPool.submit(() -> listener.consume(message));
        }
    }

    SorMessageType<MSG> getType() {
        return type;
    }

}
