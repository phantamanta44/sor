package xyz.phanta.sor.core.core;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.core.launch.NodeLaunchDelegate;
import xyz.phanta.sor.core.launch.SorInitializationException;
import xyz.phanta.sor.core.launch.SorOptions;
import xyz.phanta.sor.core.log.SorLog;

import java.io.IOException;

public class CoreLaunchDelegate extends NodeLaunchDelegate {

    private boolean offline = false;
    private String address = "";
    private int port = 7610;

    public CoreLaunchDelegate(Toml mf, SorOptions args) throws SorInitializationException {
        super(mf);
        SorLog.info("Detected core mode.");
        Toml config = mf.getTable("core");
        if (config != null) {
            SorLog.info("Parsing core settings...");
            this.offline = config.getBoolean("offline", offline);
            this.address = config.getString("address", address);
            this.port = config.getLong("port", (long)port).intValue();
        } else {
            SorLog.info("No mode-specific settings found.");
        }
    }

    @Override
    public void launch() {
        try {
            SorLog.info("Initializing nodes...");
            SorServer server = new SorServer();
            server.initializeNodes(nodes);

            if (offline) {
                SorLog.info("Offline mode; connection attempts will be ignored.");
            } else {
                server.listen(address, port);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
