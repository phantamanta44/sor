package xyz.phanta.sor.core.remote;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.core.launch.NodeLaunchDelegate;
import xyz.phanta.sor.core.launch.SorInitializationException;
import xyz.phanta.sor.core.launch.SorOptions;

public class RemoteLaunchDelegate extends NodeLaunchDelegate {

    public RemoteLaunchDelegate(Toml mf, SorOptions args) throws SorInitializationException {
        super(mf);
        throw new UnsupportedOperationException("No impl!"); // TODO impl
    }

    @Override
    public void launch() {
        throw new UnsupportedOperationException("No impl!"); // TODO impl
    }

}
