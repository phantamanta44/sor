package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.ISorApi;
import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorResponseCallback;

import javax.annotation.Nullable;
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

    @SuppressWarnings("unchecked")
    @Override
    public <MSG> SorTopic<MSG> getMessageBus(SorMessageTarget<MSG> target) {
        return (SorTopic<MSG>)topics.computeIfAbsent(target.getName(),
                k -> new SorTopic<>(target.getType(), threadPool));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <REQ, RES> SorService<REQ, RES> getRequestBus(SorRequestTarget<REQ, RES> target) {
        return (SorService<REQ, RES>)services.computeIfAbsent(target.getName(),
                k -> new SorService<>(target.getType()));
    }

    @SuppressWarnings("unchecked")
    <MSG> void post(String name, ISorMessage<MSG> msg) {
        ((SorTopic<MSG>)topics.computeIfAbsent(name, k -> new SorTopic<>(msg.getType(), threadPool))).write(msg);
    }

    @SuppressWarnings("unchecked")
    <REQ, RES> void post(String name, ISorRequest<REQ, RES> request, ISorResponseCallback<RES> callback) {
        ((SorService<REQ, RES>)services.computeIfAbsent(name, k -> new SorService<>(request.getType())))
                .request(request, callback);
    }

    @Nullable
    SorTopic<?> tryResolveTopic(String name) {
        return topics.get(name);
    }

    @Nullable
    SorService<?, ?> tryResolveService(String name) {
        return services.get(name);
    }

}
