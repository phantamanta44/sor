package xyz.phanta.sor.core.launch;

import com.moandjiezana.toml.Toml;

public interface ILaunchDelegate {

    void launch();

    @FunctionalInterface
    interface Factory {

        ILaunchDelegate create(Toml mf, SorOptions args) throws SorInitializationException;

    }

}
