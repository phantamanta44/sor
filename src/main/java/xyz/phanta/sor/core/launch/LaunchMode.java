package xyz.phanta.sor.core.launch;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.core.core.CoreLaunchDelegate;
import xyz.phanta.sor.core.remote.RemoteLaunchDelegate;

public enum LaunchMode {

    CORE(CoreLaunchDelegate::new), REMOTE(RemoteLaunchDelegate::new);

    private final ILaunchDelegate.Factory ldFactory;

    LaunchMode(ILaunchDelegate.Factory ldFactory) {
        this.ldFactory = ldFactory;
    }

    public ILaunchDelegate createLaunchDelegate(Toml mf, SorOptions args) throws SorInitializationException {
        return ldFactory.create(mf, args);
    }

}
