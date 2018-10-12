package xyz.phanta.sor.core.remote;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.core.launch.SorInitializationException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

class CoreConnectionConfiguration {

    private final String address;
    private final int port;

    CoreConnectionConfiguration(Toml conf) {
        this.address = conf.getString("address");
        if (this.address == null) throw new SorInitializationException("Core connection with no address!").wrap();
        this.port = conf.getLong("port", 7610L).intValue();
    }

    SocketAddress createAddress() {
        return InetSocketAddress.createUnresolved(address, port);
    }

}
