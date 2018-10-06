package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.ISorApi;
import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.target.ISorMessageBus;
import xyz.phanta.sor.api.target.ISorRequestBus;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

class CoreApiImpl implements ISorApi {

    private final Map<String, SorTopic<?>> topics = new Hashtable<>();
    private final Map<String, SorService<?, ?>> services = new Hashtable<>();
    private final ExecutorService threadPool;

    CoreApiImpl(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public <MSG> ISorMessageBus<MSG> getMessageBus(SorMessageTarget<MSG> target) {
        //noinspection unchecked
        return (ISorMessageBus<MSG>)topics.computeIfAbsent(target.getName(),
                k -> new SorTopic<>(target.getType(), threadPool));
    }

    @Override
    public <REQ, RES> ISorRequestBus<REQ, RES> getRequestBus(SorRequestTarget<REQ, RES> target) {
        //noinspection unchecked
        return (ISorRequestBus<REQ, RES>)services.computeIfAbsent(target.getName(),
                k -> new SorService<>(target.getType()));
    }

}
