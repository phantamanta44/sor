package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.log.SorLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO node redundancy
// TODO instrumentation api
class SorServer {

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final CoreApiImpl api = new CoreApiImpl(threadPool);

    void listen(String address, int port) throws IOException {
        SorLog.info("Opening socket...");
        Selector socketMediator = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(InetSocketAddress.createUnresolved(address, port));
        server.register(socketMediator, SelectionKey.OP_ACCEPT);

        SorLog.info("Listening for connections...");
        Map<SocketChannel, RemoteConnection> remoteConnections = new HashMap<>();
        while (!Thread.interrupted()) {
            socketMediator.select();
            Set<SelectionKey> selected = socketMediator.selectedKeys();
            for (SelectionKey key : selected) {
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        SorLog.info("Received connection from %s", client.getRemoteAddress());
                        client.configureBlocking(false);
                        client.register(socketMediator, SelectionKey.OP_READ);
                        remoteConnections.put(client, new RemoteConnection(client, api));
                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel)key.channel();
                        ByteBuffer buf = ByteBuffer.allocate(256); // TODO is this sufficient?
                        int count = client.read(buf);
                        if (count != -1) {
                            buf.limit(count);
                            RemoteConnection cxn = remoteConnections.get(client);
                            threadPool.submit(() -> {
                                try {
                                    cxn.accept(buf);
                                } catch (IllegalStateException e) {
                                    try {
                                        SorLog.warn("Remote connection %s is in bad state! Closing for safety.",
                                                client.getRemoteAddress());
                                        remoteConnections.remove(client).cleanUp();
                                        client.close();
                                        key.cancel();
                                    } catch (IOException e2) {
                                        SorLog.error("Encountered error while handling socket error!");
                                        e2.printStackTrace(System.out);
                                        // TODO fail catastrophically
                                    }
                                }
                            });
                        } else {
                            SorLog.warn("Connection terminated by %s", client.getRemoteAddress());
                            remoteConnections.remove(client).cleanUp();
                            client.close();
                            key.cancel();
                        }
                    }
                }
            }
            selected.clear();
        }
    }

    void initializeNodes(Collection<ISorNode> nodes) {
        for (ISorNode node : nodes) {
            String name = node.getClass().getCanonicalName();
            SorLog.info("Loading node %s", name);
            new Thread(() -> node.begin(api), name).start();
        }
    }

}
