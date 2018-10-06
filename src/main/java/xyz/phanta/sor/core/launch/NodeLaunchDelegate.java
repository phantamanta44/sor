package xyz.phanta.sor.core.launch;

import com.moandjiezana.toml.Toml;
import xyz.phanta.sor.api.ISorNode;
import xyz.phanta.sor.core.SorUtils;
import xyz.phanta.sor.core.log.SorLog;

import java.util.Collection;
import java.util.Collections;

public abstract class NodeLaunchDelegate implements ILaunchDelegate {

    protected final Collection<ISorNode> nodes;

    public NodeLaunchDelegate(Toml mf) throws SorInitializationException {
        try {
            SorLog.info("Loading node jars...");
            nodes = Collections.unmodifiableCollection(SorUtils.loadNodes(mf));
        } catch (SorInitializationException.Wrapped e) {
            throw e.unwrap();
        } catch (Exception e) {
            throw new SorInitializationException(e);
        }
    }

}
