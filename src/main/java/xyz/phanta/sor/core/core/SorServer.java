package xyz.phanta.sor.core.core;

import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.log.SorLog;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SorServer {

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final CoreApiImpl api = new CoreApiImpl(threadPool);

    void listen(String address, int port, boolean ssl) throws IOException {
        //noinspection MismatchedQueryAndUpdateOfCollection
        Collection<Socket> clients = Collections.synchronizedCollection(new LinkedList<>());
        SorLog.info("Starting socket polling thread...");
        Thread pollingThread = new Thread(() -> {
            SorLog.info("Socket polling thread initialized.");
            while (!Thread.interrupted()) {
                clients.removeIf(client -> {
                    try {
                        InputStream in = client.getInputStream();
                        // TODO read bytes and pass somewhere useful
                        if (client.isClosed()) {
                            SorLog.info("Connection to %s lost.", client.getRemoteSocketAddress());
                            return false;
                        }
                        return true;
                    } catch (IOException e) {
                        SorLog.error("Client %s errored; closing connection.\n", client.getRemoteSocketAddress());
                        e.printStackTrace(System.out);
                        try {
                            client.close();
                        } catch (IOException e2) {
                            SorLog.error("Encountered error while handling error:");
                            e2.printStackTrace(System.out);
                        }
                        return true;
                    }
                });
            }
        }, "socket_poll");
        pollingThread.setDaemon(true);
        pollingThread.start();
        SorLog.info("Opening socket...");
        try (ServerSocket socket = (ssl ? SSLServerSocketFactory.getDefault() : ServerSocketFactory.getDefault())
                .createServerSocket(port, 0, InetAddress.getByName(address))) {
            SorLog.info("Listening for connections...");
            //noinspection InfiniteLoopStatement
            while (!Thread.interrupted()) {
                Socket client = socket.accept();
                SorLog.info("Received connection from %s", client.getRemoteSocketAddress());
                clients.add(client);
            }
        }
        pollingThread.interrupt();
    }

    void initializeNodes(Collection<ISorNode> nodes) {
        for (ISorNode node : nodes) {
            String name = node.getClass().getCanonicalName();
            SorLog.info("Loading node %s", name);
            new Thread(() -> node.begin(api), name).start();
        }
    }

}
