package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.exchange.SorMessageTarget;
import xyz.phanta.sor.api.exchange.SorMessageType;
import xyz.phanta.sor.api.exchange.SorRequestTarget;
import xyz.phanta.sor.api.exchange.SorRequestType;
import xyz.phanta.sor.api.message.ISorMessage;
import xyz.phanta.sor.api.message.ISorMessageListener;
import xyz.phanta.sor.api.request.ISorRequest;
import xyz.phanta.sor.api.request.ISorRequestHandler;
import xyz.phanta.sor.api.request.ISorResponseCallback;
import xyz.phanta.sor.core.communication.IPacket;
import xyz.phanta.sor.core.communication.PacketWrangler;
import xyz.phanta.sor.core.communication.impl.*;
import xyz.phanta.sor.core.data.SorMessage;
import xyz.phanta.sor.core.data.SorRequest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

class RemoteConnection {

    private final PacketWrangler net;
    private final CoreApiImpl api;
    private final Map<String, RemoteTopicSub<?>> topicSubs = new HashMap<>();
    private final Map<String, RemoteServiceSub<?, ?>> serviceSubs = new HashMap<>();
    private final Map<Short, PendingRequest<?>> servicePromises = new ConcurrentHashMap<>();
    private final AtomicInteger nextServiceRequestId = new AtomicInteger(0);


    RemoteConnection(SocketChannel channel, CoreApiImpl api) {
        this.net = new PacketWrangler(channel, this::consumePacket);
        this.api = api;
    }

    void accept(ByteBuffer buf) {
        net.accept(buf);
    }

    private void consumePacket(IPacket packet) {
        try {
            if (packet instanceof PacketTopicSubscribe.Packet) {
                PacketTopicSubscribe.Packet data = (PacketTopicSubscribe.Packet)packet;
                String topicName = data.getTopicName();
                topicSubs.put(topicName, new RemoteTopicSub<>(topicName, data.getMessageType()));
            } else if (packet instanceof PacketServiceHandle.Packet) {
                PacketServiceHandle.Packet data = (PacketServiceHandle.Packet)packet;
                String serviceName = data.getServiceName();
                serviceSubs.put(serviceName,
                        new RemoteServiceSub<>(serviceName, data.getRequestType(), data.getResponseType()));
            } else if (packet instanceof PacketTopicPublish.Packet) {
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
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private <MSG> void tryPostMessage(String topicName, @Nullable SorTopic<MSG> topic, PacketTopicPublish.Packet data) {
        if (topic != null) {
            SorMessageType<MSG> msgType = topic.getType();
            api.post(topicName, new SorMessage<>(msgType, data.deserializeBody(msgType)));
        }
    }

    private <REQ, RES> void tryPostRequest(String serviceName, @Nullable SorService<REQ, RES> service,
                                           PacketServiceRequest.Packet data, ISorResponseCallback<RES> callback) {
        if (service != null) {
            SorRequestType<REQ, RES> reqType = service.getType();
            api.post(serviceName, new SorRequest<>(reqType, data.deserializeBody(reqType)), callback);
        }
    }

    void cleanUp() {
        for (RemoteTopicSub<?> listener : topicSubs.values()) listener.cleanUp();
        for (RemoteServiceSub<?, ?> handler : serviceSubs.values()) handler.cleanUp();
    }


    private class RemoteTopicSub<MSG> implements ISorMessageListener<MSG> {

        private final SorMessageTarget<MSG> topic;

        @SuppressWarnings("unchecked")
        RemoteTopicSub(String name, Class<?> msgClass) {
            this.topic = new SorMessageTarget<>(name, new SorMessageType<>((Class<MSG>)msgClass));
            api.getMessageBus(topic).listen(this);
        }

        @Override
        public void consume(ISorMessage<MSG> msg) {
            try {
                net.send(new PacketTopicPublish.Packet(topic.getName(), msg));
            } catch (IOException e) {
                throw new RuntimeException(e); // TODO handle more gracefully
            }
        }

        void cleanUp() {
            api.getMessageBus(topic).unlisten(this);
        }

    }

    private class RemoteServiceSub<REQ, RES> implements ISorRequestHandler<REQ, RES> {

        private final SorRequestTarget<REQ, RES> service;

        @SuppressWarnings("unchecked")
        RemoteServiceSub(String name, Class<?> reqClass, Class<?> resClass) {
            this.service = new SorRequestTarget<>(
                    name, new SorRequestType<>((Class<REQ>)reqClass, (Class<RES>)resClass));
            api.getRequestBus(service).handle(this);
        }

        @Nullable
        @Override
        public RES handleRequest(ISorRequest<REQ, RES> request) {
            try {
                PendingRequest<RES> promise = new PendingRequest<>(service.getType());
                short requestId = (short)(nextServiceRequestId.getAndIncrement() % 0x10000);
                servicePromises.put(requestId, promise);
                net.send(new PacketServiceRequest.Packet(requestId, service.getName(), request));
                return promise.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e); // TODO handle more gracefully
            }
        }

        void cleanUp() {
            api.getRequestBus(service).unhandle(this);
        }

    }

    private static class PendingRequest<RES> {

        private final SorRequestType<?, RES> type;
        private final CompletableFuture<RES> promise = new CompletableFuture<>();

        PendingRequest(SorRequestType<?, RES> type) {
            this.type = type;
        }

        RES get() {
            try {
                return promise.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e); // TODO handle more gracefully
            }
        }

        void complete(PacketServiceResponse.Packet data) {
            promise.complete(data.deserializeBody(type));
        }

    }

}
