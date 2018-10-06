package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.message.ISorMessageListener;
import xyz.phanta.sor.api.target.ISorMessageBus;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class SorTopic<MSG> implements ISorMessageBus<MSG> {

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

    @Override
    public void write(MSG message) {
        SorMessage<MSG> wrapped = new SorMessage<>(type, message);
        for (ISorMessageListener<MSG> listener : listeners) {
            threadPool.submit(() -> listener.consume(wrapped));
        }
    }

}
