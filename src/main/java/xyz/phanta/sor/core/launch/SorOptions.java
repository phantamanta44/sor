package xyz.phanta.sor.core.launch;

import com.beust.jcommander.Parameter;
import xyz.phanta.sor.core.log.LogLevel;

import javax.annotation.Nullable;
import java.util.Objects;

public class SorOptions {

    @Nullable
    @Parameter(description = "The launch manifest to launch SOR with.", required = true)
    private String manifest;

    @Nullable
    @Parameter(names = {"--log-level", "-l"}, description = "SOR's logging detail level.")
    private LogLevel logLevel;

    public String getManifest() {
        return Objects.requireNonNull(manifest);
    }

    @Nullable
    public LogLevel getLogLevel() {
        return logLevel;
    }

}
