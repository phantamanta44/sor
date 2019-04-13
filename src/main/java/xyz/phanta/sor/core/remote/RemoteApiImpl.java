package xyz.phanta.sor.core.remote;

import xyz.phanta.sor.api.ISorApi;
import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorResponseCallback;
import xyz.phanta.sor.core.data.SorRequest;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

class RemoteApiImpl implements ISorApi {

    private final Map<String, RemoteTopic<?>> topics = new ConcurrentHashMap<>();
    private final Map<String, RemoteService<?, ?>> services = new ConcurrentHashMap<>();
    private final ExecutorService threadPool;

    RemoteApiImpl(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    @SuppressWarnings("ConstantConditions")
    private CoreConnection connection = null;

    @SuppressWarnings("unchecked")
    @Override
    public <MSG> RemoteTopic<MSG> getMessageBus(SorMessageTarget<MSG> target) {
        return (RemoteTopic<MSG>)topics.computeIfAbsent(
                target.getName(), k -> new RemoteTopic<>(this, target, threadPool));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <REQ, RES> RemoteService<REQ, RES> getRequestBus(SorRequestTarget<REQ, RES> target) {
        return (RemoteService<REQ, RES>)services.computeIfAbsent(
                target.getName(), k -> new RemoteService<>(this, target));
    }

    void setConnection(CoreConnection connection) {
        this.connection = connection;
    }

    CoreConnection getConnection() {
        return connection;
    }

    <MSG> void post(String name, ISorMessage<MSG> msg) {
        RemoteTopic<MSG> topic = tryResolveTopic(name);
        if (topic != null) topic.write(msg);
    }

    <REQ, RES> void post(String name, ISorRequest<REQ, RES> request, ISorResponseCallback<RES> callback) {
        RemoteService<REQ, RES> service = tryResolveService(name);
        if (service != null) {
            service.request(request, callback);
        } else {
            callback.accept(new SorRequest.Reponse<>(request.getType(), null));
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    <MSG> RemoteTopic<MSG> tryResolveTopic(String topicName) {
        return (RemoteTopic<MSG>)topics.get(topicName);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    <REQ, RES> RemoteService<REQ, RES> tryResolveService(String serviceName) {
        return (RemoteService<REQ, RES>)services.get(serviceName);
    }

}
