package xyz.phanta.sor.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.core.launch.ILaunchDelegate;
import xyz.phanta.sor.core.launch.LaunchMode;
import xyz.phanta.sor.core.launch.SorInitializationException;
import xyz.phanta.sor.core.launch.SorOptions;
import xyz.phanta.sor.core.log.LogLevel;
import xyz.phanta.sor.core.log.SorLog;

import java.io.File;

public class SorMain {

    public static void main(String[] rawArgs) throws SorInitializationException {
        // parse command line args
        SorOptions args = new SorOptions();
        try {
            JCommander.newBuilder().addObject(args).build().parse(rawArgs);
        } catch (ParameterException e) {
            throw new SorInitializationException(e);
        }
        LogLevel logLevel = args.getLogLevel();
        if (logLevel != null) SorLog.setLevel(logLevel);

        // parse launch manifest and create launch delegate
        SorLog.info("Parsing launch manifest...");
        ILaunchDelegate launchDelegate;
        try {
            Toml mf = new Toml().read(new File(args.getManifest()));
            LaunchMode mode = LaunchMode.valueOf(mf.getString("mode").toUpperCase());
            SorLog.info("Creating launch delegate...");
            launchDelegate = mode.createLaunchDelegate(mf, args);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new SorInitializationException(e);
        }

        // launch
        SorLog.info("Launching SOR!");
        launchDelegate.launch();
    }

}
