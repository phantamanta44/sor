package xyz.phanta.sor.core.remote;

import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.log.SorLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

class SorRemoteServer {

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final RemoteApiImpl api = new RemoteApiImpl(threadPool);

    void connect(Collection<CoreConnectionConfiguration> cxns) throws IOException {
        // TODO core redundancy
        CoreConnectionConfiguration cxn = cxns.stream().findFirst().orElseThrow(IllegalStateException::new);

        SorLog.info("Opening socket...");
        Selector socketMediator = Selector.open();
        SocketChannel socket = SocketChannel.open(cxn.createAddress());
        CoreConnection coreCxn = new CoreConnection(socket, api);
        api.setConnection(coreCxn);
        socket.configureBlocking(false);
        socket.register(socketMediator, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

        AtomicBoolean active = new AtomicBoolean(true);
        while (active.get() && !Thread.interrupted()) {
            socketMediator.select();
            Set<SelectionKey> selected = socketMediator.selectedKeys();
            for (SelectionKey key : selected) {
                if (key.isConnectable()) {
                    SorLog.info("Established connection to %s", socket.getRemoteAddress());
                } else if (key.isReadable()) {
                    ByteBuffer buf = ByteBuffer.allocate(256);
                    int count = socket.read(buf);
                    if (count != -1) {
                        buf.limit(count);
                        threadPool.submit(() -> {
                            try {
                                coreCxn.accept(buf);
                            } catch (IllegalStateException e) {
                                try {
                                    SorLog.error("Connection to %s is in bad state!", socket.getRemoteAddress());
                                    socket.close();
                                    socketMediator.close();
                                } catch (Exception e2) {
                                    SorLog.error("Encountered error while handling socket error!");
                                    e2.printStackTrace(System.out);
                                } finally {
                                    active.set(false);
                                }
                            }
                        });
                    } else {
                        SorLog.error("Connection terminated by %s", socket.getRemoteAddress());
                        socket.close();
                        socketMediator.close();
                        break;
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
