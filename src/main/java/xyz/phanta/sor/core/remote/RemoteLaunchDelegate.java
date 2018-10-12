package xyz.phanta.sor.core.remote;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.core.launch.NodeLaunchDelegate;
import xyz.phanta.sor.core.launch.SorInitializationException;
import xyz.phanta.sor.core.launch.SorOptions;
import xyz.phanta.sor.core.log.SorLog;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteLaunchDelegate extends NodeLaunchDelegate {

    private final Collection<CoreConnectionConfiguration> coreCxnConfigs;

    public RemoteLaunchDelegate(Toml mf, SorOptions args) throws SorInitializationException {
        super(mf);
        SorLog.info("Detected remote mode.");
        List<Toml> cores = mf.getTables("core");
        if (cores == null || cores.isEmpty()) {
            throw new SorInitializationException("No core connections configured!");
        }
        SorLog.info("Parsing core connections...");
        try {
            coreCxnConfigs = cores.stream()
                    .map(CoreConnectionConfiguration::new)
                    .collect(Collectors.toList());
        } catch (SorInitializationException.Wrapped e) {
            throw e.unwrap();
        }
    }

    @Override
    public void launch() {
        try {
            SorLog.info("Initializing nodes...");
            SorRemoteServer remote = new SorRemoteServer();
            remote.initializeNodes(nodes);

            remote.connect(coreCxnConfigs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
