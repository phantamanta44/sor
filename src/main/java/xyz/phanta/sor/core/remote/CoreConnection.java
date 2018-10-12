package xyz.phanta.sor.core.remote;

import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.request.ISorResponseCallback;
import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.PacketWrangler;
import xyz.phanta.sor.core.communication.impl.*;
import xyz.phanta.sor.core.data.SorMessage;
import xyz.phanta.sor.core.data.SorRequest;
import xyz.phanta.sor.core.log.SorLog;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class CoreConnection {

    private final PacketWrangler net;
    private final RemoteApiImpl api;
    private final Map<Short, PendingRequest<?>> servicePromises = new ConcurrentHashMap<>();
    private final AtomicInteger nextServiceRequestId = new AtomicInteger(0);

    CoreConnection(SocketChannel socket, RemoteApiImpl api) {
        this.net = new PacketWrangler(socket, this::consumePacket);
        this.api = api;
    }

    void accept(ByteBuffer buf) {
        net.accept(buf);
    }

    private void consumePacket(IPacket packet) {
        if (packet instanceof PacketTopicPublish.Packet) {
            PacketTopicPublish.Packet data = (PacketTopicPublish.Packet)packet;
            String topicName = data.getTopicName();
            tryPostMessage(topicName, api.tryResolveTopic(topicName), data);
        } else if (packet instanceof PacketServiceRequest.Packet) {
            PacketServiceRequest.Packet data = (PacketServiceRequest.Packet)packet;
            String serviceName = data.getServiceName();
            tryPostRequest(serviceName, api.tryResolveService(serviceName), data, res -> {
                try {
                    net.send(new PacketServiceResponse.Packet(data.getRequestId(), res));
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO handle more gracefully
                }
            });
        } else if (packet instanceof PacketServiceResponse.Packet) {
            PacketServiceResponse.Packet data = (PacketServiceResponse.Packet)packet;
            servicePromises.remove(data.getRequestId()).complete(data);
        } else {
            throw new IllegalArgumentException("Illegal packet type: " + packet.getClass().getCanonicalName());
        }
    }

    private <MSG> void tryPostMessage(String topicName, @Nullable RemoteTopic<MSG> topic,
                                      PacketTopicPublish.Packet data) {
        if (topic != null) {
            SorMessageType<MSG> msgType = topic.getType();
            api.post(topicName, new SorMessage<>(msgType, data.deserializeBody(msgType)));
        }
    }

    private <REQ, RES> void tryPostRequest(String serviceName, @Nullable RemoteService<REQ, RES> service,
                                           PacketServiceRequest.Packet data, ISorResponseCallback<RES> callback) {
        if (service != null) {
            SorRequestType<REQ, RES> reqType = service.getType();
            api.post(serviceName, new SorRequest<>(reqType, data.deserializeBody(reqType)), callback);
        }
    }

    <MSG> void publishTopic(SorMessageTarget<MSG> topic, MSG message) {
        safeSend(new PacketTopicPublish.Packet(topic.getName(), new SorMessage<>(topic.getType(), message)));
    }

    void listenTopic(SorMessageTarget<?> topic) {
        safeSend(new PacketTopicSubscribe.Packet(topic.getName(), topic.getType().getMessageClass()));
    }

    <REQ, RES> void publishService(SorRequestTarget<REQ, RES> service, REQ request,
                                   ISorResponseCallback<RES> callback) {
        PendingRequest<RES> promise = new PendingRequest<>(service.getType(), callback);
        short requestId = (short)(nextServiceRequestId.getAndIncrement() % 0x10000);
        servicePromises.put(requestId, promise);
        safeSend(new PacketServiceRequest.Packet(
                requestId, service.getName(), new SorRequest<>(service.getType(), request)));
    }

    void listenService(SorRequestTarget<?, ?> service) {
        SorRequestType<?, ?> type = service.getType();
        safeSend(new PacketServiceHandle.Packet(service.getName(), type.getRequestClass(), type.getResponseClass()));
    }

    private void safeSend(IPacket packet) {
        try {
            net.send(packet);
        } catch (IOException e) {
            SorLog.warn("Failed to send packet of type %s", packet.getClass().getCanonicalName());
            e.printStackTrace(System.out);
        }
    }

    private static class PendingRequest<RES> {

        private final SorRequestType<?, RES> type;
        private final CompletableFuture<RES> promise = new CompletableFuture<>();

        PendingRequest(SorRequestType<?, RES> type, ISorResponseCallback<RES> callback) {
            this.type = type;
            this.promise.whenComplete((result, error) -> {
                if (error != null) {
                    SorLog.warn("Request errored!");
                    error.printStackTrace(System.out);
                    callback.accept(new SorRequest.Reponse<>(type, null));
                } else {
                    callback.accept(new SorRequest.Reponse<>(type, result));
                }
            });
        }

        void complete(PacketServiceResponse.Packet data) {
            promise.complete(data.deserializeBody(type));
        }

    }


}
